# 📄 Mobility Data Pipeline & Dashboard PoC - Design Document

**Version:** 1.4 (Updated: Tech Stack Refinement - Kotlin, MySQL)
**Methodology:** SDD (Spec Driven Development) & TDD (Test Driven Development)
**Tooling:** Antigravity IDE, JUnit5, KotlinTest(선택)

---

## 1. Project Overview (프로젝트 개요)

- **목표:** 다양한 모빌리티 및 파워트레인 특성을 반영한 주행 데이터를 생성하고, RDBMS에 영구 저장(Persistence)함과 동시에 프론트엔드 대시보드로 실시간 스트리밍(SSE)하는 Full-stack 시스템 구축.
- **핵심 가치:**
  - **Kotlin & Spring Boot:** 간결하고 안전한 도메인 모델링 및 현대적인 백엔드 생태계 활용.
  - **MySQL (Persistence):** 실시간 스트리밍뿐만 아니라 시계열(Time-series) 형태의 주행 로그 데이터 영구 저장 및 조회 기반 마련.
  - **Full-stack Type Safety:** 백엔드(Kotlin/Java)부터 프론트엔드(TypeScript)까지 이어지는 견고한 타입 시스템 구축.
- **기술 스택:**
  - **Backend:** Kotlin, Java, Spring Boot, Spring Data JPA, Spring Web (SseEmitter) (server 디렉토리)
  - **Database:** MySQL
  - **Frontend:** Next.js, React, TypeScript, EventSource API (client 디렉토리)

## 2. System Architecture (시스템 아키텍처)

1. **[Data Generator]** Kotlin 스케줄러(`@Scheduled`)를 통해 다형성(EV/ICE)을 띤 가상 주행 데이터 주기적 생성.
2. **[Persistence]** 생성된 데이터를 Spring Data JPA를 통해 MySQL `vehicle_logs` 테이블에 Insert.
3. **[Event Streaming]** DB 저장과 동시에(혹은 트랜잭션 종료 후) ApplicationEventPublisher를 통해 SSE Emitter로 데이터 Push.
4. **[Dashboard UI]** Next.js에서 실시간 데이터를 받아 지도 마커 갱신 및 상태바 렌더링.

## 3. Core Specifications: Phase 1 (Backend - Kotlin & Spring Boot & MySQL)

### Spec 1.1: Domain Modeling with Kotlin (코틀린 기반 도메인 모델링)

- Kotlin의 `sealed interface` 또는 `sealed class`를 활용하여 `Vehicle` 도메인을 설계한다.
  - `EvVehicle`: 배터리 잔량(`batteryLevel`) 속성 보유.
  - `IceVehicle`: 연료 잔량(`fuelLevel`) 속성 보유.
- DTO는 불변성을 보장하는 Kotlin의 `data class`로 구성하여 Java 대비 보일러플레이트 코드를 획기적으로 줄인다.

### Spec 1.2: RDBMS Persistence (MySQL 데이터 저장)

- MySQL에 `vehicle_logs` 테이블을 설계하고, JPA Entity를 매핑한다.
- 초당 발생하는 데이터를 DB에 효율적으로 Insert 하기 위한 배치 처리(Batch Insert) 또는 비동기 저장을 고려한다.

### Spec 1.3: Real-time SSE Broadcasting (실시간 브로드캐스팅)

- 데이터를 MySQL에 적재함과 동시에 연결된 클라이언트들에게 SSE(`text/event-stream`)로 JSON 페이로드를 Push 한다.

## 4. Core Specifications: Phase 2 (Frontend - Next.js & React & TS)

### Spec 1.4: Real-time State Management

- `EventSource` API로 백엔드와 연결하고, 수신된 데이터를 React State(또는 Zustand/Redux 등)에 안전하게 업데이트한다.
- TypeScript의 유니온 타입(Union Types)과 타입 가드(Type Guards)를 활용해 백엔드에서 넘어온 EV/ICE 데이터를 런타임 에러 없이 분기 처리한다.

### Spec 1.5: Detailed Dashboard Visualization

- **지도 UI:** 차량 타입(`REGULAR`, `FREIGHT`, `MICRO`)별 맞춤형 아이콘 렌더링.
- **상태 패널:** 온도 표시 및 파워트레인에 따른 에너지 게이지 바(Battery vs Fuel) 동적 렌더링.

## 5. Data Model (데이터 페이로드 및 DB 스키마 규격)

Kotlin `data class`로 직렬화될 JSON 규격이자 MySQL 테이블 컬럼의 기반입니다.

```json
{
  "logId": "Long (Auto Increment, MySQL PK)",
  "vehicleId": "String (ex. REG-EV-001)",
  "vehicleType": "String (REGULAR | FREIGHT | MICRO)",
  "powertrain": "String (EV | ICE)",
  "latitude": 37.5283,
  "longitude": 127.0335,
  "speed": 60,
  "temperature": 22.5,
  "batteryLevel": 85, // ICE인 경우 null, DB 컬럼도 nullable
  "fuelLevel": null, // EV인 경우 null, DB 컬럼도 nullable
  "createdAt": "LocalDateTime (DB Insert 시간)"
}
```
