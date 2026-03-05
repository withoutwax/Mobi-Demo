package com.modapl.poc.infrastructure

import com.modapl.poc.domain.Vehicle
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue

/**
 * [Spec 1.2] 실시간 차량 데이터 인메모리 버퍼
 *
 * 목적:
 * - 생산자(VehicleDataGenerator)가 생성한 데이터를 임시 저장
 * - 소비자(Data Ingestion Service)가 비동기적으로 데이터를 꺼내어 DB 저장 및 SSE 발송 처리
 *
 * 스레드 안전성(Thread-safety):
 * - LinkedBlockingQueue를 사용하여 다수의 생산자/소비자 스레드가 동시에 접근해도
 *   데드락이나 데이터 유실, ConcurrentModificationException 없이 정합성을 보장한다.
 */
@Component
class VehicleDataBuffer {

    companion object {
        // 급증하는 트래픽에 대비하되 메모리 초과를 방지하기 위한 최대 용량 제한
        private const val CAPACITY = 1000
    }

    private val buffer = LinkedBlockingQueue<Vehicle>(CAPACITY)

    /**
     * 버퍼에 차량 데이터를 추가한다.
     * 용량이 꽉 찼을 경우 IllegalStateException을 던진다.
     * (현재 PoC 수준에서는 버리고 예외 처리, 운영 환경에서는 다른 정책 가능)
     *
     * @param vehicle 저장할 파워트레인 도메인 객체 (EvVehicle / IceVehicle)
     * @throws IllegalStateException 버퍼가 꽉 찼을 때
     */
    fun offer(vehicle: Vehicle) {
        val success = buffer.offer(vehicle)
        if (!success) {
            throw IllegalStateException("VehicleDataBuffer capacity($CAPACITY) exceeded.")
        }
    }

    /**
     * 다중 데이터를 한 번에 추가하기 위한 편의 메서드.
     */
    fun offerAll(vehicles: List<Vehicle>) {
        vehicles.forEach { offer(it) }
    }

    /**
     * 버퍼에서 큐 구조(FIFO)에 따라 차량 데이터를 꺼낸다.
     * 버퍼가 비어있으면 null을 반환한다. (Non-blocking)
     *
     * @return 꺼낸 Vehicle 객체 또는 버퍼가 비었을 시 null
     */
    fun poll(): Vehicle? {
        return buffer.poll()
    }

    /**
     * 현재 버퍼에 대기 중인 데이터 건수를 반환한다.
     */
    fun size(): Int {
        return buffer.size
    }
}
