package com.mobi.poc.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [Spec 1.1] Vehicle 도메인 모델 단위 테스트 (TDD - Red Phase)
 *
 * 검증 대상:
 * - Vehicle sealed interface + EvVehicle / IceVehicle data class 다형성
 * - 에너지 필드 분리 (batteryLevel / fuelLevel)
 * - VehicleType.MICRO + Powertrain.ICE 조합 제한
 */
@DisplayName("Vehicle 도메인 모델 규칙 검증")
class VehicleDomainTest {

    // =========================================================
    // 공통 픽스처
    // =========================================================
    private fun makeEvVehicle(
        vehicleType: VehicleType = VehicleType.REGULAR,
        batteryLevel: Double = 80.0,
    ): EvVehicle = EvVehicle(
        vehicleId    = "REG-EV-001",
        vehicleType  = vehicleType,
        latitude     = 37.5283,
        longitude    = 127.0335,
        speed        = 60.0,
        temperature  = 22.5,
        batteryLevel = batteryLevel,
    )

    private fun makeIceVehicle(
        vehicleType: VehicleType = VehicleType.REGULAR,
        fuelLevel: Double = 70.0,
    ): IceVehicle = IceVehicle(
        vehicleId   = "REG-ICE-001",
        vehicleType = vehicleType,
        latitude    = 37.5283,
        longitude   = 127.0335,
        speed       = 60.0,
        temperature = 22.5,
        fuelLevel   = fuelLevel,
    )

    // =========================================================
    // 1. 다형성 및 공통 속성 접근 검증
    // =========================================================
    @Nested
    @DisplayName("1. 다형성 및 공통 속성 접근")
    inner class PolymorphismTest {

        @Test
        @DisplayName("EvVehicle과 IceVehicle은 Vehicle 타입으로 다룰 수 있다")
        fun `Vehicle sealed interface 다형성 - 공통 타입으로 참조 가능`() {
            // Given
            val ev: Vehicle  = makeEvVehicle()
            val ice: Vehicle = makeIceVehicle()

            // When & Then
            assertThat(ev).isInstanceOf(Vehicle::class.java)
            assertThat(ice).isInstanceOf(Vehicle::class.java)
        }

        @Test
        @DisplayName("Vehicle 타입으로 5가지 공통 속성에 접근할 수 있다")
        fun `Vehicle 타입으로 공통 속성 접근 - vehicleId, latitude, longitude, speed, temperature`() {
            // Given
            val vehicles: List<Vehicle> = listOf(makeEvVehicle(), makeIceVehicle())

            // When & Then
            vehicles.forEach { vehicle ->
                assertThat(vehicle.vehicleId).isNotBlank()
                assertThat(vehicle.latitude).isBetween(-90.0, 90.0)
                assertThat(vehicle.longitude).isBetween(-180.0, 180.0)
                assertThat(vehicle.speed).isGreaterThanOrEqualTo(0.0)
                assertThat(vehicle.temperature).isNotNull()
            }
        }
    }

    // =========================================================
    // 2. 에너지 필드 분리 검증
    // =========================================================
    @Nested
    @DisplayName("2. 에너지 필드 분리")
    inner class EnergyFieldTest {

        @Test
        @DisplayName("EvVehicle은 batteryLevel을 가지며, fuelLevel 필드를 가지지 않는다")
        fun `EvVehicle 에너지 필드 - batteryLevel만 존재해야 한다`() {
            // Given
            val ev = makeEvVehicle(batteryLevel = 85.0)

            // When & Then
            assertThat(ev.batteryLevel).isEqualTo(85.0)
            // EvVehicle 클래스에 fuelLevel 프로퍼티가 없음을 컴파일 레벨에서 보장
            // (아래 코드가 컴파일되면 실패: ev.fuelLevel)
        }

        @Test
        @DisplayName("IceVehicle은 fuelLevel을 가지며, batteryLevel 필드를 가지지 않는다")
        fun `IceVehicle 에너지 필드 - fuelLevel만 존재해야 한다`() {
            // Given
            val ice = makeIceVehicle(fuelLevel = 70.0)

            // When & Then
            assertThat(ice.fuelLevel).isEqualTo(70.0)
            // IceVehicle 클래스에 batteryLevel 프로퍼티가 없음을 컴파일 레벨에서 보장
            // (아래 코드가 컴파일되면 실패: ice.batteryLevel)
        }

        @Test
        @DisplayName("when 식으로 Vehicle 타입 분기 시 에너지 값을 정확히 추출할 수 있다")
        fun `sealed interface when 분기 - 에너지 필드를 타입 안전하게 추출`() {
            // Given
            val vehicles: List<Vehicle> = listOf(
                makeEvVehicle(batteryLevel = 90.0),
                makeIceVehicle(fuelLevel = 55.0),
            )

            // When
            val energyValues = vehicles.map { vehicle ->
                when (vehicle) {
                    is EvVehicle  -> vehicle.batteryLevel
                    is IceVehicle -> vehicle.fuelLevel
                }
            }

            // Then
            assertThat(energyValues).containsExactly(90.0, 55.0)
        }
    }

    // =========================================================
    // 3. 파워트레인 조합 제한 — 정상 케이스
    // =========================================================
    @Nested
    @DisplayName("3. 파워트레인 조합 제한 (정상 케이스)")
    inner class ValidPowertrainCombinationTest {

        @Test
        @DisplayName("REGULAR 타입 차량은 EV와 ICE 모두 정상 생성된다")
        fun `REGULAR 차량 - EV와 ICE 모두 생성 가능`() {
            // Given & When
            val regularEv  = makeEvVehicle(vehicleType = VehicleType.REGULAR)
            val regularIce = makeIceVehicle(vehicleType = VehicleType.REGULAR)

            // Then
            assertThat(regularEv.vehicleType).isEqualTo(VehicleType.REGULAR)
            assertThat(regularIce.vehicleType).isEqualTo(VehicleType.REGULAR)
        }

        @Test
        @DisplayName("FREIGHT 타입 차량은 EV와 ICE 모두 정상 생성된다")
        fun `FREIGHT 차량 - EV와 ICE 모두 생성 가능`() {
            // Given & When
            val freightEv  = makeEvVehicle(vehicleType = VehicleType.FREIGHT)
            val freightIce = makeIceVehicle(vehicleType = VehicleType.FREIGHT)

            // Then
            assertThat(freightEv.vehicleType).isEqualTo(VehicleType.FREIGHT)
            assertThat(freightIce.vehicleType).isEqualTo(VehicleType.FREIGHT)
        }

        @Test
        @DisplayName("MICRO 타입 차량은 EV로 정상 생성된다")
        fun `MICRO 차량 - EV는 생성 가능`() {
            // Given & When
            val microEv = makeEvVehicle(vehicleType = VehicleType.MICRO)

            // Then
            assertThat(microEv.vehicleType).isEqualTo(VehicleType.MICRO)
        }
    }

    // =========================================================
    // 4. 파워트레인 조합 제한 — 예외 케이스
    // =========================================================
    @Nested
    @DisplayName("4. 파워트레인 조합 제한 (예외 케이스)")
    inner class InvalidPowertrainCombinationTest {

        @Test
        @DisplayName("MICRO 타입에 ICE 파워트레인 조합은 IllegalArgumentException을 발생시킨다")
        fun `MICRO ICE 조합 - 도메인 불변식 위반 시 예외 발생`() {
            // Given
            val invalidVehicleType = VehicleType.MICRO

            // When & Then
            assertThatThrownBy {
                IceVehicle(
                    vehicleId   = "MICRO-ICE-INVALID",
                    vehicleType = invalidVehicleType,
                    latitude    = 37.5283,
                    longitude   = 127.0335,
                    speed       = 15.0,
                    temperature = 22.5,
                    fuelLevel   = 50.0,
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("MICRO")
        }
    }
}
