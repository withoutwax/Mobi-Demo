package com.modapl.poc.domain

// ============================================================
// [Spec 1.1] Vehicle 도메인 모델
// - sealed interface를 통한 EV/ICE 다형성 구현
// - data class를 통한 불변성 보장
// - init { require(...) }를 통한 Fail-fast 도메인 유효성 검증
// ============================================================

/**
 * 차량 종류 (VehicleType)
 * - REGULAR  : 일반 승용차
 * - FREIGHT  : 화물차
 * - MICRO    : 마이크로 모빌리티 (킥보드/자전거) — EV 전용
 */
enum class VehicleType { REGULAR, FREIGHT, MICRO }

/**
 * 파워트레인 구분
 * - 직접 필드로 갖는 것이 아니라 Vehicle 구현체 타입 자체가 파워트레인을 나타낸다.
 * - EvVehicle → EV / IceVehicle → ICE (Data Model의 "powertrain" 필드에 대응)
 */
enum class Powertrain { EV, ICE }

/**
 * Vehicle sealed interface
 *
 * 공통 속성: [Spec 1.1] 및 [Data Model] 기반
 * - vehicleId   : 차량 ID (예: REG-EV-001)
 * - vehicleType : 차량 종류 (REGULAR | FREIGHT | MICRO)
 * - powertrain  : 파워트레인 (EV | ICE) — 구현체 타입에서 결정
 * - latitude    : 위도
 * - longitude   : 경도
 * - speed       : 속도 (km/h)
 * - temperature : 온도 (°C)
 */
sealed interface Vehicle {
    val vehicleId: String
    val vehicleType: VehicleType
    val powertrain: Powertrain
    val latitude: Double
    val longitude: Double
    val speed: Double
    val temperature: Double
}

/**
 * EV (Electric Vehicle) 구현체
 *
 * 추가 속성: batteryLevel (0~100 %)
 * fuelLevel 없음 — data class에 미포함으로 컴파일 타임 보장
 *
 * 도메인 불변식:
 * - batteryLevel ∈ [0.0, 100.0]
 */
data class EvVehicle(
    override val vehicleId: String,
    override val vehicleType: VehicleType,
    override val latitude: Double,
    override val longitude: Double,
    override val speed: Double,
    override val temperature: Double,
    val batteryLevel: Double,
) : Vehicle {

    override val powertrain: Powertrain = Powertrain.EV

    init {
        require(batteryLevel in 0.0..100.0) {
            "batteryLevel은 0~100 사이여야 합니다. (입력값: $batteryLevel)"
        }
    }
}

/**
 * ICE (Internal Combustion Engine) 구현체
 *
 * 추가 속성: fuelLevel (0~100 %)
 * batteryLevel 없음 — data class에 미포함으로 컴파일 타임 보장
 *
 * 도메인 불변식:
 * - vehicleType ≠ MICRO  (마이크로 모빌리티는 EV 전용)
 * - fuelLevel ∈ [0.0, 100.0]
 */
data class IceVehicle(
    override val vehicleId: String,
    override val vehicleType: VehicleType,
    override val latitude: Double,
    override val longitude: Double,
    override val speed: Double,
    override val temperature: Double,
    val fuelLevel: Double,
) : Vehicle {

    override val powertrain: Powertrain = Powertrain.ICE

    init {
        require(vehicleType != VehicleType.MICRO) {
            "MICRO 타입 차량은 ICE 파워트레인을 가질 수 없습니다. (vehicleType=$vehicleType)"
        }
        require(fuelLevel in 0.0..100.0) {
            "fuelLevel은 0~100 사이여야 합니다. (입력값: $fuelLevel)"
        }
    }
}
