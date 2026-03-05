# 🏛️ System Architecture Design: Mobility Data Pipeline

이 문서는 PoC 프로젝트의 기술적 의사결정, 데이터 흐름, 그리고 객체 지향 설계 원칙을 상세히 다룹니다.

## 1. High-Level System Design

전체 시스템은 **생산자(Vehicle Simulator) - 중계자(Spring Boot) - 소비자(Next.js Dashboard)**의 3단계 구조로 설계되었습니다.

```mermaid
graph LR
    subgraph "Vehicle Simulation (Producer)"
        A[VehicleDataProducer] -->|@Scheduled 1s| B[VehicleDataGenerator]
        B --> C{VehicleType}
        C -->|REGULAR/FREIGHT EV| D[EvVehicle]
        C -->|REGULAR/FREIGHT ICE| E[IceVehicle]
        C -->|MICRO EV 고정| D
        D & E -->|offerAll| F[VehicleDataBuffer]
    end

    subgraph "Backend Engine (Spring Boot)"
        E[Data Ingestion Service] --> F[(MySQL)]
        E --> G[SSE Emitter Service]
    end

    subgraph "Real-time Dashboard (Consumer)"
        G -->|text/event-stream| H[EventSource API]
        H --> I[React State / Map UI]
    end
```

---

## 3. Producer Layer: VehicleDataGenerator ([Spec 1.1])

`@Component`로 등록된 `VehicleDataGenerator`는 `generate()` 호출당 **최소 5대**의 가상 주행 데이터를 생성합니다.

### 3.1. 생성 전략 (Fixed Composition)

| VehicleType | Powertrain  | 대수 | 속도 범위  |
| ----------- | ----------- | ---- | ---------- |
| REGULAR     | EV          | 1대  | 30~80 km/h |
| REGULAR     | ICE         | 1대  | 30~80 km/h |
| FREIGHT     | EV          | 1대  | 40~60 km/h |
| FREIGHT     | ICE         | 1대  | 40~60 km/h |
| MICRO       | **EV 고정** | 1대  | 10~25 km/h |

> **MICRO+ICE 방어**: MICRO 타입은 항상 `createEvVehicle()`을 호출하여 도메인 불변식(`IceVehicle.init` 예외)을 트리거하지 않도록 설계.

### 3.2. 실시간 스케줄링 및 버퍼 적재 (`VehicleDataProducer`)

- `@Scheduled(fixedRate = 1000)` 어노테이션을 사용하여 **1초(1000ms)** 단위로 위 생성 전략에 따라 데이터를 생산합니다.
- 생성된 데이터는 `VehicleDataBuffer`(thread-safe `LinkedBlockingQueue(1000)`)에 `offerAll`로 삽입됩니다.
- **Fault-tolerance:** 인메모리 버퍼가 가득 차서(Capacity 1000 초과) `IllegalStateException`이 발생할 경우, 스케줄러 스레드가 죽는 것을 방지하기 위해 내부적으로 예외를 캐치(catch) 후 `Warning` 로그만 남기며 다음 1초 주기를 정상적으로 대기합니다.

### 3.3. 좌표 생성 범위

- **위도(latitude):** `37.4 ~ 37.7` (서울 시내)
- **경도(longitude):** `126.8 ~ 127.2` (서울 시내)
- `kotlin.random.Random.nextDouble(min, max)`으로 매 호출마다 다른 좌표 생성.

---

## 2. Domain Modeling: Vehicle Polymorphism ([Spec 1.1])

`sealed interface`를 활용한 EV/ICE 다형성 모델입니다. **컴파일 타임 타입 안전성**과 **생성 시점 유효성 검증(Fail-fast)**을 동시에 보장합니다.

```mermaid
classDiagram
    class Vehicle {
        <<sealed interface>>
        +vehicleId: String
        +vehicleType: VehicleType
        +powertrain: Powertrain
        +latitude: Double
        +longitude: Double
        +speed: Double
        +temperature: Double
    }

    class EvVehicle {
        <<data class>>
        +batteryLevel: Double
        +powertrain = EV
        init() require(batteryLevel in 0..100)
    }

    class IceVehicle {
        <<data class>>
        +fuelLevel: Double
        +powertrain = ICE
        init() require(vehicleType != MICRO)
        init() require(fuelLevel in 0..100)
    }

    class VehicleType {
        <<enum>>
        REGULAR
        FREIGHT
        MICRO
    }

    class Powertrain {
        <<enum>>
        EV
        ICE
    }

    Vehicle <|.. EvVehicle
    Vehicle <|.. IceVehicle
    Vehicle --> VehicleType
    Vehicle --> Powertrain
```

### 핵심 설계 결정

| 결정                                 | 이유                                                                                  |
| ------------------------------------ | ------------------------------------------------------------------------------------- |
| `sealed interface` 사용              | `when` 분기 시 컴파일러가 모든 하위 타입을 강제 처리 → 런타임 오류 원천 차단          |
| `data class` 사용                    | 불변성(`val`) + `equals/hashCode/copy` 자동 생성으로 보일러플레이트 제거              |
| `powertrain`을 computed `val`로 선언 | 구현체 타입과 파워트레인의 불일치를 컴파일 타임에 방지                                |
| `init { require(...) }` 사용         | 도메인 불변식을 생성 시점에 즉시 검증(Fail-fast), 잘못된 상태의 객체가 생성될 수 없음 |
| MICRO + ICE 조합 금지                | MICRO 모빌리티(킥보드/자전거)는 물리적으로 연소 엔진 탑재 불가                        |
