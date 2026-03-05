package com.mobi.poc.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Vehicle Domain Model 검증")
class VehicleDomainTest {

    // --- 1. 다형성 및 타입 검증 (5) ---
    @Test
    @DisplayName("1. EvVehicle 인스턴스 생성 시 타입이 EV 파워트레인으로 지정되는지 검증")
    fun `ev vehicle powertrain is EV`() {
        val ev = EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
        assertThat(ev.powertrain).isEqualTo(Powertrain.EV)
    }

    @Test
    @DisplayName("2. IceVehicle 인스턴스 생성 시 타입이 ICE 파워트레인으로 지정되는지 검증")
    fun `ice vehicle powertrain is ICE`() {
        val ice = IceVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 50.0)
        assertThat(ice.powertrain).isEqualTo(Powertrain.ICE)
    }

    @Test
    @DisplayName("3. VehicleType.REGULAR로 지정 시 정상 생성 검증")
    fun `regular type creation`() {
        val ev = EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
        assertThat(ev.vehicleType).isEqualTo(VehicleType.REGULAR)
    }

    @Test
    @DisplayName("4. VehicleType.FREIGHT로 지정 시 정상 생성 검증")
    fun `freight type creation`() {
        val ice = IceVehicle("ID", VehicleType.FREIGHT, 37.0, 127.0, 50.0, 20.0, 50.0)
        assertThat(ice.vehicleType).isEqualTo(VehicleType.FREIGHT)
    }

    @Test
    @DisplayName("5. VehicleType.MICRO로 지정 (EvVehicle) 시 정상 생성 검증")
    fun `micro type ev vehicle creation`() {
        val ev = EvVehicle("ID", VehicleType.MICRO, 37.0, 127.0, 20.0, 20.0, 100.0)
        assertThat(ev.vehicleType).isEqualTo(VehicleType.MICRO)
    }

    // --- 2. 제약사항 및 예외 방어 (Fail-fast) 검증 (6) ---
    @Test
    @DisplayName("6. 불변식 위반: IceVehicle에 MICRO 타입을 지정 시 IllegalArgumentException 발생")
    fun `micro type ice vehicle throws exception`() {
        assertThatThrownBy {
            IceVehicle("ID", VehicleType.MICRO, 37.0, 127.0, 20.0, 20.0, 50.0)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("7. EvVehicle의 배터리 레벨이 0.0일 때 무사 통과 (경계값)")
    fun `ev battery level 0 is allowed`() {
        val ev = EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 0.0)
        assertThat(ev.batteryLevel).isEqualTo(0.0)
    }

    @Test
    @DisplayName("8. EvVehicle의 배터리 레벨이 100.0일 때 무사 통과 (경계값)")
    fun `ev battery level 100 is allowed`() {
        val ev = EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
        assertThat(ev.batteryLevel).isEqualTo(100.0)
    }

    @Test
    @DisplayName("9. EvVehicle의 배터리 레벨이 -0.1일 때 예외 발생 (음수 불가)")
    fun `ev battery level negative throws exception`() {
        assertThatThrownBy {
            EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, -0.1)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("10. EvVehicle의 배터리 레벨이 100.1일 때 예외 발생 (100 초과 불가)")
    fun `ev battery level over 100 throws exception`() {
        assertThatThrownBy {
            EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.1)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("11. IceVehicle의 연료 레벨이 허용 구간을 벗어났을 때 예외 발생 (-1.0, 101.0 등)")
    fun `ice fuel level out of bounds throws exception`() {
        assertThatThrownBy {
            IceVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, -1.0)
        }.isInstanceOf(IllegalArgumentException::class.java)
        
        assertThatThrownBy {
            IceVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 101.0)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    // --- 3. 필드 정합성 및 Kotlin Data Class 특성 검증 (6) ---
    @Test
    @DisplayName("12. 공통 프로퍼티(latitude, longitude, speed, temperature, vehicleId) 접근 검증")
    fun `common properties access`() {
        val ev: Vehicle = EvVehicle("TEST-ID", VehicleType.REGULAR, 37.5, 127.5, 60.0, 25.0, 80.0)
        assertThat(ev.vehicleId).isEqualTo("TEST-ID")
        assertThat(ev.latitude).isEqualTo(37.5)
        assertThat(ev.longitude).isEqualTo(127.5)
        assertThat(ev.speed).isEqualTo(60.0)
        assertThat(ev.temperature).isEqualTo(25.0)
    }

    @Test
    @DisplayName("13. EvVehicle 객체의 동등성(equals) 검증 (데이터가 같으면 true)")
    fun `ev vehicle equality`() {
        val ev1 = EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
        val ev2 = EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
        assertThat(ev1).isEqualTo(ev2)
    }

    @Test
    @DisplayName("14. IceVehicle 객체의 동등성(equals) 검증")
    fun `ice vehicle equality`() {
        val ice1 = IceVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 50.0)
        val ice2 = IceVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 50.0)
        assertThat(ice1).isEqualTo(ice2)
    }

    @Test
    @DisplayName("15. EvVehicle 객체의 copy()를 통한 불변 객체 복사 검증")
    fun `ev vehicle copy`() {
        val ev1 = EvVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
        val ev2 = ev1.copy(speed = 60.0)
        assertThat(ev2.speed).isEqualTo(60.0)
        assertThat(ev2.vehicleId).isEqualTo("ID")
    }

    @Test
    @DisplayName("16. copy() 시나리오: ID만 변경했을 때 나머지가 유지되고 동등성은 false인지 검증")
    fun `copy with different id`() {
        val ev1 = EvVehicle("ID1", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
        val ev2 = ev1.copy(vehicleId = "ID2")
        assertThat(ev1).isNotEqualTo(ev2)
        assertThat(ev1.latitude).isEqualTo(ev2.latitude)
    }

    @Test
    @DisplayName("17. hashCode() 구현 규칙 검증: 동일한 데이터를 가진 두 인스턴스가 같은 해시코드를 반환하는지 검증")
    fun `hashcode consistency`() {
        val ice1 = IceVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 50.0)
        val ice2 = IceVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 50.0)
        assertThat(ice1.hashCode()).isEqualTo(ice2.hashCode())
    }

    // --- 4. 특수한 상태 (Edge Case) (3) ---
    @Test
    @DisplayName("18. 극단적인 위경도값(음수 등) 입력 시에도 안전하게 생성되는지 확인")
    fun `extreme coordinates generation`() {
        val ev = EvVehicle("ID", VehicleType.REGULAR, -90.0, -180.0, 50.0, 20.0, 100.0)
        assertThat(ev.latitude).isEqualTo(-90.0)
        assertThat(ev.longitude).isEqualTo(-180.0)
    }

    @Test
    @DisplayName("19. 속도(speed)가 0인 정차 상태 생성 검증")
    fun `speed zero is allowed`() {
        val ice = IceVehicle("ID", VehicleType.REGULAR, 37.0, 127.0, 0.0, 20.0, 50.0)
        assertThat(ice.speed).isEqualTo(0.0)
    }

    @Test
    @DisplayName("20. 차량 ID가 빈 문자열(\"\")일 때 생성 동작 확인 (필요시 제약 추가 제안용)")
    fun `empty vehicle id generation`() {
        val ev = EvVehicle("", VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
        assertThat(ev.vehicleId).isEmpty()
    }
}
