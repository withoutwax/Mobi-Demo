# 📄 Mobility Data Pipeline & Dashboard PoC - Design Document

**Version:** 1.5 (Updated: Docker Compose 기반 컨테이너 환경 추가)
**Methodology:** SDD (Spec Driven Development) & TDD (Test Driven Development)
**Tooling:** Antigravity IDE, JUnit5, KotlinTest(선택), Docker & Docker Compose

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
  - **Infra / DevOps:** Docker, Docker Compose

## 2. System Architecture (시스템 아키텍처)

1. **[Data Generator]** Kotlin 스케줄러(`@Scheduled`)를 통해 다형성(EV/ICE)을 띤 가상 주행 데이터 주기적 생성.
2. **[Persistence]** 생성된 데이터를 Spring Data JPA를 통해 MySQL `vehicle_logs` 테이블에 Insert.
3. **[Event Streaming]** DB 저장과 동시에(혹은 트랜잭션 종료 후) ApplicationEventPublisher를 통해 SSE Emitter로 데이터 Push.
4. **[Dashboard UI]** Next.js에서 실시간 데이터를 받아 지도 마커 갱신 및 상태바 렌더링.
5. **[Container Orchestration]** 위 1~4의 모든 서비스를 Docker Compose로 오케스트레이션하여 `docker compose up` 한 명령어로 전체 시스템 기동.

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

## 6. Container Specifications: Docker Compose (컨테이너 환경 명세)

### Spec 6.1: Service Definitions (서비스 정의)

프로젝트 루트의 `docker-compose.yml` 파일에 아래 세 가지 서비스를 정의한다.

| Service  | Image / Build                | Port Mapping | 비고                                |
| -------- | ---------------------------- | ------------ | ----------------------------------- |
| `db`     | `mysql:8.0`                  | `3306:3306`  | 볼륨 마운트로 데이터 영속화         |
| `server` | `./server` (Dockerfile 빌드) | `8080:8080`  | `db` 서비스 의존 (`depends_on`)     |
| `client` | `./client` (Dockerfile 빌드) | `3000:3000`  | `server` 서비스 의존 (`depends_on`) |

### Spec 6.2: MySQL Container (DB 컨테이너)

- 공식 `mysql:8.0` 이미지를 사용한다.
- 환경 변수(`MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE` 등)를 `.env` 파일 또는 `environment` 블록으로 관리한다.
- `volumes`를 통해 데이터를 Named Volume(`db-data`)에 영속화하여 컨테이너 재시작 시 데이터 유실을 방지한다.
- `healthcheck`를 설정하여 MySQL이 완전히 기동된 후 백엔드가 연결을 시도하도록 보장한다.

### Spec 6.3: Backend Container (Spring Boot 컨테이너)

- `server/Dockerfile`에 Multi-stage Build를 적용한다.
  - **Stage 1 (Builder):** Gradle 빌드를 통해 실행 가능한 JAR 생성.
  - **Stage 2 (Runtime):** 경량 JRE 이미지(`eclipse-temurin:17-jre-alpine` 등) 위에 JAR 배치.
- `application.yml` 또는 환경 변수로 DB 접속 정보를 Docker 내부 네트워크 기준(`db:3306`)으로 설정한다.
- `depends_on` + `healthcheck` 조건을 통해 MySQL 기동 완료 후 Spring Boot가 시작되도록 순서를 보장한다.

### Spec 6.4: Frontend Container (Next.js 컨테이너)

- `client/Dockerfile`에 Multi-stage Build를 적용한다.
  - **Stage 1 (Builder):** `npm install && npm run build`로 프로덕션 번들 생성.
  - **Stage 2 (Runtime):** `node:18-alpine` 이미지 위에서 `npm start`로 서비스 기동.
- SSE 엔드포인트 URL을 환경 변수(`NEXT_PUBLIC_API_URL`)로 주입받아 Docker 네트워크 내 `server` 서비스에 연결한다.

### Spec 6.5: Network & Environment (네트워크 및 환경 변수)

- Docker Compose의 기본 브리지 네트워크를 활용하여 서비스 간 통신한다 (서비스명으로 DNS 해석).
- 민감 정보(DB 비밀번호 등)는 `.env` 파일로 분리하고, `.gitignore`에 등록하여 소스 관리에서 제외한다.
- `.env.example` 파일을 제공하여 다른 개발자가 환경을 쉽게 구성할 수 있도록 한다.

### Spec 6.6: Development Workflow (개발 워크플로우)

```bash
# 전체 시스템 기동 (빌드 포함)
docker compose up --build

# 백그라운드 실행
docker compose up -d

# 로그 확인
docker compose logs -f server

# 전체 종료 및 볼륨 정리
docker compose down -v
```
