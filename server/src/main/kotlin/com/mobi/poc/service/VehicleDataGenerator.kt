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

    private val counter = AtomicInteger(0)

    // 서울 시내 유효 좌표 범위
    private val seoulLatRange = 37.4..37.7
    private val seoulLngRange = 126.8..127.2

    // 차량 타입별 속도 범위 (km/h)
    private val regularSpeedRange = 30.0..80.0
    private val freightSpeedRange = 40.0..60.0
    private val microSpeedRange   = 10.0..25.0

    // 온도 범위 (°C)
    private val temperatureRange  = -10.0..40.0

    /**
     * 최소 5대의 가상 주행 데이터를 생성하여 반환한다.
     *
     * 구성:
     * - REGULAR 2대 (EV 1대 + ICE 1대)
     * - FREIGHT 2대 (EV 1대 + ICE 1대)
     * - MICRO   1대 (EV 고정)
     */
    fun generate(): List<Vehicle> = buildList {
        // REGULAR: EV + ICE 각 1대 보장
        add(createEvVehicle(VehicleType.REGULAR))
        add(createIceVehicle(VehicleType.REGULAR))

        // FREIGHT: EV + ICE 각 1대 보장
        add(createEvVehicle(VehicleType.FREIGHT))
        add(createIceVehicle(VehicleType.FREIGHT))

        // MICRO: 항상 EvVehicle (도메인 불변식 - ICE 금지)
        add(createEvVehicle(VehicleType.MICRO))
    }

    // ------------------------------------------------------------------
    // Private Factory Methods
    // ------------------------------------------------------------------

    private fun createEvVehicle(type: VehicleType): EvVehicle {
        val seq = counter.incrementAndGet()
        return EvVehicle(
            vehicleId    = "${type.name}-EV-%03d".format(seq),
            vehicleType  = type,
            latitude     = randomInRange(seoulLatRange),
            longitude    = randomInRange(seoulLngRange),
            speed        = randomInRange(speedRangeFor(type)),
            temperature  = randomInRange(temperatureRange),
            batteryLevel = randomInRange(10.0..100.0),
        )
    }

    private fun createIceVehicle(type: VehicleType): IceVehicle {
        val seq = counter.incrementAndGet()
        return IceVehicle(
            vehicleId   = "${type.name}-ICE-%03d".format(seq),
            vehicleType = type,
            latitude    = randomInRange(seoulLatRange),
            longitude   = randomInRange(seoulLngRange),
            speed       = randomInRange(speedRangeFor(type)),
            temperature = randomInRange(temperatureRange),
            fuelLevel   = randomInRange(10.0..100.0),
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
