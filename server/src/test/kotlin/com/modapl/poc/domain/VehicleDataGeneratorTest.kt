package com.modapl.poc.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [Spec 1.1] VehicleDataGenerator 단위 테스트 (TDD - Red Phase)
 *
 * 검증 대상:
 * - generate() 호출 시 최소 5대 이상 차량 리스트 반환
 * - MICRO 타입은 항상 EvVehicle(EV 파워트레인) 이어야 함
 * - 차량 타입별 speed 범위 준수
 * - 생성 좌표가 서울 시내 유효 반경 내에 위치
 */
@DisplayName("VehicleDataGenerator 생성 규칙 검증")
class VehicleDataGeneratorTest {

    private lateinit var generator: VehicleDataGenerator

    // 서울 시내 유효 좌표 범위
    private val seoulLatRange  = 37.4..37.7
    private val seoulLngRange  = 126.8..127.2

    // 차량 타입별 속도 범위
    private val regularSpeedRange = 30.0..80.0
    private val freightSpeedRange = 40.0..60.0
    private val microSpeedRange   = 10.0..25.0

    @BeforeEach
    fun setUp() {
        generator = VehicleDataGenerator()
    }

    // =========================================================
    // 1. 생성 볼륨 검증
    // =========================================================
    @Nested
    @DisplayName("1. 생성 볼륨")
    inner class GenerationVolumeTest {

        @Test
        @DisplayName("generate()를 한 번 호출하면 최소 5대 이상의 차량 데이터 리스트가 반환된다")
        fun `generate 호출 - 최소 5대 이상 차량 반환`() {
            // Given
            // VehicleDataGenerator가 준비된 상태

            // When
            val vehicles: List<Vehicle> = generator.generate()

            // Then
            assertThat(vehicles)
                .isNotEmpty
                .hasSizeGreaterThanOrEqualTo(5)
        }
    }

    // =========================================================
    // 2. 차량 타입 및 파워트레인 조합 검증
    // =========================================================
    @Nested
    @DisplayName("2. 차량 타입 및 파워트레인 조합")
    inner class PowertrainCombinationTest {

        @Test
        @DisplayName("MICRO 타입 차량은 반드시 EvVehicle(EV 파워트레인)이어야 한다")
        fun `MICRO 타입 파워트레인 제약 - 모든 MICRO 차량은 EvVehicle`() {
            // Given
            val vehicles: List<Vehicle> = generator.generate()

            // When
            val microVehicles = vehicles.filter { it.vehicleType == VehicleType.MICRO }

            // Then
            // MICRO 차량이 없는 경우 조건 자체가 vacuously true — 최소 1대를 보장하려면
            // 구현체가 MICRO 차량을 항상 포함하도록 설계되어야 함
            assertThat(microVehicles).isNotEmpty

            microVehicles.forEach { vehicle ->
                assertThat(vehicle)
                    .withFailMessage("MICRO 타입 차량이 EvVehicle이 아닙니다: $vehicle")
                    .isInstanceOf(EvVehicle::class.java)
            }
        }

        @Test
        @DisplayName("생성된 모든 차량은 REGULAR, FREIGHT, MICRO 중 하나의 타입을 가진다")
        fun `생성 차량 타입 - 전체 타입이 VehicleType 범위 안에 있다`() {
            // Given
            val vehicles: List<Vehicle> = generator.generate()

            // When & Then
            vehicles.forEach { vehicle ->
                assertThat(vehicle.vehicleType)
                    .isIn(VehicleType.REGULAR, VehicleType.FREIGHT, VehicleType.MICRO)
            }
        }
    }

    // =========================================================
    // 3. 속도 범위 검증
    // =========================================================
    @Nested
    @DisplayName("3. 차량 타입별 속도 범위")
    inner class SpeedRangeTest {

        @Test
        @DisplayName("REGULAR 타입 차량의 speed는 30~80km/h 범위 내에 있어야 한다")
        fun `REGULAR 차량 speed - 30 이상 80 이하`() {
            // Given
            val vehicles: List<Vehicle> = generator.generate()

            // When
            val regularVehicles = vehicles.filter { it.vehicleType == VehicleType.REGULAR }

            // Then
            assertThat(regularVehicles).isNotEmpty
            regularVehicles.forEach { vehicle ->
                assertThat(vehicle.speed)
                    .withFailMessage("REGULAR 차량의 speed(${vehicle.speed})가 범위를 벗어났습니다.")
                    .isBetween(regularSpeedRange.start, regularSpeedRange.endInclusive)
            }
        }

        @Test
        @DisplayName("FREIGHT 타입 차량의 speed는 40~60km/h 범위 내에 있어야 한다")
        fun `FREIGHT 차량 speed - 40 이상 60 이하`() {
            // Given
            val vehicles: List<Vehicle> = generator.generate()

            // When
            val freightVehicles = vehicles.filter { it.vehicleType == VehicleType.FREIGHT }

            // Then
            assertThat(freightVehicles).isNotEmpty
            freightVehicles.forEach { vehicle ->
                assertThat(vehicle.speed)
                    .withFailMessage("FREIGHT 차량의 speed(${vehicle.speed})가 범위를 벗어났습니다.")
                    .isBetween(freightSpeedRange.start, freightSpeedRange.endInclusive)
            }
        }

        @Test
        @DisplayName("MICRO 타입 차량의 speed는 10~25km/h 범위 내에 있어야 한다")
        fun `MICRO 차량 speed - 10 이상 25 이하`() {
            // Given
            val vehicles: List<Vehicle> = generator.generate()

            // When
            val microVehicles = vehicles.filter { it.vehicleType == VehicleType.MICRO }

            // Then
            assertThat(microVehicles).isNotEmpty
            microVehicles.forEach { vehicle ->
                assertThat(vehicle.speed)
                    .withFailMessage("MICRO 차량의 speed(${vehicle.speed})가 범위를 벗어났습니다.")
                    .isBetween(microSpeedRange.start, microSpeedRange.endInclusive)
            }
        }
    }

    // =========================================================
    // 4. 좌표 범위 검증
    // =========================================================
    @Nested
    @DisplayName("4. 서울 시내 좌표 범위")
    inner class CoordinateRangeTest {

        @Test
        @DisplayName("생성된 모든 차량의 위도는 37.4~37.7 범위 내에 있어야 한다")
        fun `위도 범위 - 서울 시내 유효 반경 (37_4 ~ 37_7)`() {
            // Given
            val vehicles: List<Vehicle> = generator.generate()

            // When & Then
            vehicles.forEach { vehicle ->
                assertThat(vehicle.latitude)
                    .withFailMessage("위도(${vehicle.latitude})가 서울 유효 범위를 벗어났습니다.")
                    .isBetween(seoulLatRange.start, seoulLatRange.endInclusive)
            }
        }

        @Test
        @DisplayName("생성된 모든 차량의 경도는 126.8~127.2 범위 내에 있어야 한다")
        fun `경도 범위 - 서울 시내 유효 반경 (126_8 ~ 127_2)`() {
            // Given
            val vehicles: List<Vehicle> = generator.generate()

            // When & Then
            vehicles.forEach { vehicle ->
                assertThat(vehicle.longitude)
                    .withFailMessage("경도(${vehicle.longitude})가 서울 유효 범위를 벗어났습니다.")
                    .isBetween(seoulLngRange.start, seoulLngRange.endInclusive)
            }
        }
    }
}
