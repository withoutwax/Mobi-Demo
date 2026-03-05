package com.mobi.poc.service

import com.mobi.poc.infrastructure.VehicleDataBuffer
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * [Spec 1.1] 실시간 차량 데이터 생산 스케줄러 (Producer)
 *
 * 주기적으로(VehicleDataGenerator)를 호출하여 가상 데이터를 생성하고,
 * 이를 버퍼(VehicleDataBuffer)에 적재한다.
 */
@Component
class VehicleDataProducer(
    private val vehicleDataGenerator: VehicleDataGenerator,
    private val vehicleDataBuffer: VehicleDataBuffer
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 1초(1000ms)마다 주기적으로 실행되어 차량 데이터를 버퍼에 밀어 넣는다.
     *
     * 버퍼가 가득 차서 예외가 발생하더라도, 애플리케이션이나 스케줄러 스레드가
     * 죽지 않도록 내부에서 예외를 캐치(catch)하고 로깅만 수행한다.
     */
    @Scheduled(fixedRate = 1000)
    fun produceData() {
        try {
            val newVehicles = vehicleDataGenerator.generate()
            vehicleDataBuffer.offerAll(newVehicles)
            log.debug("Successfully produced and buffered {} vehicles.", newVehicles.size)
        } catch (e: IllegalStateException) {
            // 버퍼 용량 초과 등 비즈니스 예외 발생 시 스케줄러가 죽지 않게 방어
            log.warn("Failed to buffer vehicles: {}", e.message)
        } catch (e: Exception) {
            // 그 외 예기치 않은 예외 방어
            log.error("Unexpected error during vehicle data production", e)
        }
    }
}
