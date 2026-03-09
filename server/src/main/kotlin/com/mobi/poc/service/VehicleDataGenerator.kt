package com.mobi.poc.service

import com.mobi.poc.domain.*
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * [Spec 1.1] SDV 복합 데이터 생성기
 *
 * - 호출당 최소 5대의 차량 데이터 생성 (REGULAR 2대, FREIGHT 2대, MICRO 1대 + 랜덤 추가)
 * - 차량 타입별 속도 범위 보장
 * - 서울 시내 유효 좌표 범위 보장
 * - MICRO 타입은 반드시 EvVehicle로 생성 (도메인 불변식 준수)
 */
@Component
class VehicleDataGenerator {

    // 서울 시내 유효 좌표 범위
    private val seoulLatRange = 37.4..37.7
    private val seoulLngRange = 126.8..127.2

    // 차량 타입별 속도 범위 (km/h)
    private val regularSpeedRange = 30.0..80.0
    private val freightSpeedRange = 40.0..60.0
    private val microSpeedRange   = 10.0..25.0

    // 온도 범위 (°C)
    private val temperatureRange  = -10.0..40.0

    // 고정된 차량 5대 풀 유지
    private val fixedVehicles: List<Vehicle> = listOf(
        // REGULAR: EV + ICE 각 1대 보장
        initEvVehicle("REGULAR-EV-001", VehicleType.REGULAR),
        initIceVehicle("REGULAR-ICE-002", VehicleType.REGULAR),
        // FREIGHT: EV + ICE 각 1대 보장
        initEvVehicle("FREIGHT-EV-003", VehicleType.FREIGHT),
        initIceVehicle("FREIGHT-ICE-004", VehicleType.FREIGHT),
        // MICRO: 항상 EvVehicle (도메인 불변식 - ICE 금지)
        initEvVehicle("MICRO-EV-005", VehicleType.MICRO)
    )

    /**
     * 고정된 5대의 가상 주행 데이터를 업데이트하여 반환한다.
     * ID는 고정 유지되며 위치, 속도, 배터리/연료 등의 상태만 변경된다.
     */
    fun generate(): List<Vehicle> = fixedVehicles.map { vehicle ->
        when (vehicle) {
            is EvVehicle -> updateEvVehicle(vehicle)
            is IceVehicle -> updateIceVehicle(vehicle)
        }
    }

    // ------------------------------------------------------------------
    // Private Factory Methods
    // ------------------------------------------------------------------

    private fun initEvVehicle(id: String, type: VehicleType): EvVehicle {
        return EvVehicle(
            vehicleId    = id,
            vehicleType  = type,
            latitude     = randomInRange(seoulLatRange),
            longitude    = randomInRange(seoulLngRange),
            speed        = randomInRange(speedRangeFor(type)),
            temperature  = randomInRange(temperatureRange),
            batteryLevel = randomInRange(10.0..100.0),
        )
    }

    private fun initIceVehicle(id: String, type: VehicleType): IceVehicle {
        return IceVehicle(
            vehicleId   = id,
            vehicleType = type,
            latitude    = randomInRange(seoulLatRange),
            longitude   = randomInRange(seoulLngRange),
            speed       = randomInRange(speedRangeFor(type)),
            temperature = randomInRange(temperatureRange),
            fuelLevel   = randomInRange(10.0..100.0),
        )
    }

    private fun updateEvVehicle(vehicle: EvVehicle): EvVehicle {
        return vehicle.copy(
            // 실제 주행처럼 위치를 미세하게 변동시킬 수도 있지만, 여기서는 무작위 갱신으로 둠
            latitude     = randomInRange(seoulLatRange),
            longitude    = randomInRange(seoulLngRange),
            speed        = randomInRange(speedRangeFor(vehicle.vehicleType)),
            temperature  = randomInRange(temperatureRange),
            // 배터리는 조금씩 닳게 하거나 랜덤 변동
            batteryLevel = randomInRange(10.0..100.0)
        )
    }

    private fun updateIceVehicle(vehicle: IceVehicle): IceVehicle {
        return vehicle.copy(
            latitude    = randomInRange(seoulLatRange),
            longitude   = randomInRange(seoulLngRange),
            speed       = randomInRange(speedRangeFor(vehicle.vehicleType)),
            temperature = randomInRange(temperatureRange),
            fuelLevel   = randomInRange(10.0..100.0)
        )
    }

    private fun speedRangeFor(type: VehicleType): ClosedFloatingPointRange<Double> = when (type) {
        VehicleType.REGULAR -> regularSpeedRange
        VehicleType.FREIGHT -> freightSpeedRange
        VehicleType.MICRO   -> microSpeedRange
    }

    private fun randomInRange(range: ClosedFloatingPointRange<Double>): Double =
        Random.nextDouble(range.start, range.endInclusive)
}
