package com.mobi.poc.service

import com.mobi.poc.domain.Vehicle
import com.mobi.poc.infrastructure.VehicleDataBuffer
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * [Spec 1.3] 실시간 차량 데이터 소비자 (Consumer / Broadcaster)
 *
 * 주기적으로 인메모리 버퍼(VehicleDataBuffer)에서 데이터를 꺼내와
 * 접속 중인 클라이언트들에게 브로드캐스팅(SseEmitterService)한다.
 */
@Component
class VehicleDataBroadcaster(
    private val vehicleDataBuffer: VehicleDataBuffer,
    private val sseEmitterService: SseEmitterService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 1초(1000ms)마다 주기적으로 실행되어 버퍼의 데이터를 SSE로 쏜다.
     *
     * 접속자 수가 0명이면 불필요한 직렬화/전송을 막기 위해 Early Return 한다.
     */
    @Scheduled(fixedRate = 1000)
    fun broadcastData() {
        try {
            // 접속자(Subscriber) 유무 확인
            if (sseEmitterService.getEmitterCount() == 0) {
                // 접속자가 없을 때의 버퍼 관리 정책:
                // 버퍼에서 데이터를 비우지(poll) 않고 그대로 둡니다.
                // 생산자(VehicleDataProducer)가 계속 데이터를 넣다 보면 버퍼가 꽉 차게 되는데,
                // 이 때 Producer 측에서 Capacity Exceeded 예외를 catch하여 Drop-newest 방식으로
                // 자연스럽게 데이터를 버리게 됩니다. (운영 메모리 초과 방어)
                return
            }

            // 버퍼에 쌓여있는 모든 데이터를 꺼냄 (Drain)
            val vehiclesToBroadcast = mutableListOf<Vehicle>()
            while (true) {
                val vehicle = vehicleDataBuffer.poll() ?: break
                vehiclesToBroadcast.add(vehicle)
            }

            // 꺼내온 데이터가 있을 때만 브로드캐스팅 수행
            if (vehiclesToBroadcast.isNotEmpty()) {
                sseEmitterService.broadcast(vehiclesToBroadcast)
                log.debug("Broadcasted {} vehicles to {} clients", vehiclesToBroadcast.size, sseEmitterService.getEmitterCount())
            }

        } catch (e: Exception) {
            // 브로드캐스팅 중 예기치 않은 예외가 발생하더라도 스케줄러 스레드가 죽지 않도록 방어 (Fault tolerance)
            log.error("Unexpected error during vehicle data broadcasting", e)
        }
    }
}
