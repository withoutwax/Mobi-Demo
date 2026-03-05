package com.mobi.poc.service

import com.mobi.poc.domain.EvVehicle
import com.mobi.poc.domain.IceVehicle
import com.mobi.poc.domain.Powertrain
import com.mobi.poc.domain.Vehicle
import com.mobi.poc.domain.VehicleType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@DisplayName("VehicleDataGenerator 동작 20종 검증")
class VehicleDataGeneratorTest {

    private lateinit var generator: VehicleDataGenerator

    @BeforeEach
    fun setUp() {
        generator = VehicleDataGenerator()
    }

    // --- 1. 생성 볼륨 및 타입 조합 (6) ---
    @Test
    @DisplayName("1. generate() 호출 시 반환 리스트 사이즈가 최소 5 이상인지 검증")
    fun `generate returns at least 5 vehicles`() {
        val result = generator.generate()
        assertThat(result).hasSizeGreaterThanOrEqualTo(5)
    }

    @Test
    @DisplayName("2. 반환 리스트 내에 REGULAR 타입이 최소 2대(EV 1, ICE 1) 존재하는지 검증")
    fun `contains at least 2 REGULAR vehicles`() {
        val result = generator.generate()
        val regularV = result.filter { it.vehicleType == VehicleType.REGULAR }
        assertThat(regularV).hasSizeGreaterThanOrEqualTo(2)
        assertThat(regularV.map { it.powertrain }).contains(Powertrain.EV, Powertrain.ICE)
    }

    @Test
    @DisplayName("3. 반환 리스트 내에 FREIGHT 타입이 최소 2대(EV 1, ICE 1) 존재하는지 검증")
    fun `contains at least 2 FREIGHT vehicles`() {
        val result = generator.generate()
        val freightV = result.filter { it.vehicleType == VehicleType.FREIGHT }
        assertThat(freightV).hasSizeGreaterThanOrEqualTo(2)
        assertThat(freightV.map { it.powertrain }).contains(Powertrain.EV, Powertrain.ICE)
    }

    @Test
    @DisplayName("4. 반환 리스트 내에 MICRO 타입이 최소 1대 존재하는지 검증")
    fun `contains at least 1 MICRO vehicle`() {
        val result = generator.generate()
        val microV = result.filter { it.vehicleType == VehicleType.MICRO }
        assertThat(microV).hasSizeGreaterThanOrEqualTo(1)
    }

    @Test
    @DisplayName("5. MICRO 타입은 항상 EvVehicle 인스턴스인지 검증")
    fun `MICRO is always EV`() {
        val result = generator.generate()
        val microV = result.filter { it.vehicleType == VehicleType.MICRO }
        microV.forEach { 
            assertThat(it).isInstanceOf(EvVehicle::class.java)
            assertThat(it.powertrain).isEqualTo(Powertrain.EV)
        }
    }

    @Test
    @DisplayName("6. 각 차량의 ID가 포맷을 일관되게 유지하는지 정규식 검증")
    fun `vehicle id format validation`() {
        val result = generator.generate()
        result.forEach {
            assertThat(it.vehicleId).matches("^[A-Z]+-(EV|ICE)-\\d+$")
        }
    }

    // --- 2. 좌표 생성 경계 검증 (4) ---
    @Test
    @DisplayName("7. 생성된 모든 차량의 위도가 37.4 이상인지 검증")
    fun `latitude min bound`() {
        val result = generator.generate()
        result.forEach { assertThat(it.latitude).isGreaterThanOrEqualTo(37.4) }
    }

    @Test
    @DisplayName("8. 생성된 모든 차량의 위도가 37.7 이하인지 검증")
    fun `latitude max bound`() {
        val result = generator.generate()
        result.forEach { assertThat(it.latitude).isLessThanOrEqualTo(37.7) }
    }

    @Test
    @DisplayName("9. 생성된 모든 차량의 경도가 126.8 이상인지 검증")
    fun `longitude min bound`() {
        val result = generator.generate()
        result.forEach { assertThat(it.longitude).isGreaterThanOrEqualTo(126.8) }
    }

    @Test
    @DisplayName("10. 생성된 모든 차량의 경도가 127.2 이하인지 검증")
    fun `longitude max bound`() {
        val result = generator.generate()
        result.forEach { assertThat(it.longitude).isLessThanOrEqualTo(127.2) }
    }

    // --- 3. 속도(Speed) 및 온도 규칙 검증 (5) ---
    @Test
    @DisplayName("11. REGULAR 차량 속도가 30.0 ~ 80.0 사이인지 검증")
    fun `regular speed bounds`() {
        val result = generator.generate()
        result.filter { it.vehicleType == VehicleType.REGULAR }.forEach {
            assertThat(it.speed).isBetween(30.0, 80.0)
        }
    }

    @Test
    @DisplayName("12. FREIGHT 차량 속도가 40.0 ~ 60.0 사이인지 검증")
    fun `freight speed bounds`() {
        val result = generator.generate()
        result.filter { it.vehicleType == VehicleType.FREIGHT }.forEach {
            assertThat(it.speed).isBetween(40.0, 60.0)
        }
    }

    @Test
    @DisplayName("13. MICRO 차량 속도가 10.0 ~ 25.0 사이인지 검증")
    fun `micro speed bounds`() {
        val result = generator.generate()
        result.filter { it.vehicleType == VehicleType.MICRO }.forEach {
            assertThat(it.speed).isBetween(10.0, 25.0)
        }
    }

    @Test
    @DisplayName("14. 온도 값이 -10.0 ~ 40.0 범위 내에 위치하는지 검증")
    fun `temperature bounds`() {
        val result = generator.generate()
        result.forEach {
            assertThat(it.temperature).isBetween(-10.0, 40.0)
        }
    }

    @Test
    @DisplayName("15. 배터리 및 연료 레벨이 10.0 ~ 100.0 범위 내에 위치하는지 검증")
    fun `energy level bounds`() {
        val result = generator.generate()
        result.forEach {
            when (it) {
                is EvVehicle -> assertThat(it.batteryLevel).isBetween(10.0, 100.0)
                is IceVehicle -> assertThat(it.fuelLevel).isBetween(10.0, 100.0)
            }
        }
    }

    // --- 4. 고부하/반복 상태 시의 무결성 (5) ---
    @Test
    @DisplayName("16. generate() 메서드를 10회 연속 호출해도 예외 없이 매번 5대 이상을 반환하는가")
    fun `multiple generate calls`() {
        for (i in 1..10) {
            assertThat(generator.generate()).hasSizeGreaterThanOrEqualTo(5)
        }
    }

    @Test
    @DisplayName("17. 여러 번 호출 시 차량 ID의 순번이 계속 증가하여 중복 ID가 발생하지 않는지 검증")
    fun `id sequence increments`() {
        val idSet = mutableSetOf<String>()
        for (i in 1..5) {
            val result = generator.generate()
            result.forEach { 
                assertThat(idSet.add(it.vehicleId)).isTrue()
            }
        }
    }

    @Test
    @DisplayName("18. 병렬 스레드(100개)에서 동시에 generate() 호출 시 ID 충돌 방지 여부")
    fun `concurrent ID generation without duplicates`() {
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(100)
        val totalVehicles = java.util.concurrent.ConcurrentLinkedQueue<Vehicle>()

        for (i in 1..100) {
            executor.submit {
                totalVehicles.addAll(generator.generate())
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()

        val idList = totalVehicles.map { it.vehicleId }
        val idSet = idList.toSet()
        assertThat(idList.size).isEqualTo(idSet.size) // 중복 없음
    }

    @Test
    @DisplayName("19. 랜덤 값이 고정되지 않고 매번 호출 시 배터리/연료 수치가 변경되는지 검증")
    fun `randomized values differ per call`() {
        val r1 = generator.generate()
        val r2 = generator.generate()
        val b1 = r1.filterIsInstance<EvVehicle>().map { it.batteryLevel }
        val b2 = r2.filterIsInstance<EvVehicle>().map { it.batteryLevel }
        // 테스트 취약점 방지: 두 랜덤 배열이 완벽히 똑같을 확률은 희박
        assertThat(b1).isNotEqualTo(b2)
    }

    @Test
    @DisplayName("20. 반환된 리스트에 null 데이터가 없는지 정합성 검증")
    fun `no null elements in list`() {
        val result = generator.generate()
        assertThat(result).doesNotContainNull()
    }
}
