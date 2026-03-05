package com.modapl.poc.service

import com.modapl.poc.domain.EvVehicle
import com.modapl.poc.domain.Vehicle
import com.modapl.poc.domain.VehicleType
import com.modapl.poc.infrastructure.VehicleDataBuffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * [Spec 1.1] VehicleDataBuffer 동시성(Thread-Safety) 통합 테스트
 *
 * 검증 대상:
 * - 100개 스레드 동시 offer() → 데이터 유실 없는 정합성
 * - 생산자/소비자 혼합 경합 → 데드락 및 ConcurrentModificationException 부재
 */
@DisplayName("VehicleDataBuffer 동시성(Thread-Safety) 검증")
class VehicleDataBufferConcurrencyTest {

    private lateinit var buffer: VehicleDataBuffer

    @BeforeEach
    fun setUp() {
        buffer = VehicleDataBuffer()
    }

    // =========================================================
    // 픽스처 — 테스트용 더미 차량 생성
    // =========================================================
    private fun sampleVehicle(id: Int): Vehicle = EvVehicle(
        vehicleId    = "TEST-EV-%03d".format(id),
        vehicleType  = VehicleType.REGULAR,
        latitude     = 37.5283,
        longitude    = 127.0335,
        speed        = 50.0,
        temperature  = 22.0,
        batteryLevel = 80.0,
    )

    // =========================================================
    // 1. 동시 쓰기: 100개 스레드 → 데이터 유실 없는 개수 정합성
    // =========================================================
    @Test
    @DisplayName("100개 스레드가 동시에 offer()를 호출해도 데이터 유실 없이 정확한 개수가 저장된다")
    fun `동시 쓰기 - 100개 스레드 offer 후 count 정합성 검증`() {
        // Given
        val threadCount  = 100
        val executor     = Executors.newFixedThreadPool(threadCount)
        val startLatch   = CountDownLatch(1)           // 일제 출발 게이트
        val doneLatch    = CountDownLatch(threadCount)  // 전원 완료 대기

        // When — 100개 스레드가 startLatch.countDown() 신호를 받고 일제히 offer()
        repeat(threadCount) { i ->
            executor.submit {
                try {
                    startLatch.await()          // 모든 스레드가 준비될 때까지 대기
                    buffer.offer(sampleVehicle(i))
                } finally {
                    doneLatch.countDown()
                }
            }
        }
        startLatch.countDown()                          // 출발 신호
        val completed = doneLatch.await(10, TimeUnit.SECONDS)

        executor.shutdown()

        // Then
        assertThat(completed)
            .withFailMessage("10초 내에 모든 스레드가 완료되지 않았습니다 (데드락 의심)")
            .isTrue()
        assertThat(buffer.size())
            .withFailMessage("Race Condition으로 인해 데이터가 유실되었습니다.")
            .isEqualTo(threadCount)
    }

    // =========================================================
    // 2. 생산자-소비자 혼합 경합: 데드락 & CME 부재 검증
    // =========================================================
    @Test
    @DisplayName("생산자와 소비자가 동시에 동작해도 데드락이나 ConcurrentModificationException이 발생하지 않는다")
    fun `생산자-소비자 동시성 - 데드락 및 예외 없이 정상 완료`() {
        // Given
        val producerCount  = 50
        val consumerCount  = 50
        val totalThreads   = producerCount + consumerCount
        val executor       = Executors.newFixedThreadPool(totalThreads)
        val startLatch     = CountDownLatch(1)
        val doneLatch      = CountDownLatch(totalThreads)
        val producedCount  = AtomicInteger(0)
        val consumedCount  = AtomicInteger(0)

        // When & Then — 예외 없이 종료되어야 함
        assertThatCode {

            // 생산자 스레드 × 50
            repeat(producerCount) { i ->
                executor.submit {
                    try {
                        startLatch.await()
                        buffer.offer(sampleVehicle(i))
                        producedCount.incrementAndGet()
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            // 소비자 스레드 × 50
            repeat(consumerCount) {
                executor.submit {
                    try {
                        startLatch.await()
                        if (buffer.poll() != null) consumedCount.incrementAndGet()
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            startLatch.countDown()                                      // 출발 신호
            val completed = doneLatch.await(10, TimeUnit.SECONDS)

            // 타임아웃 없이 완료 = 데드락 없음 증명
            assertThat(completed)
                .withFailMessage("10초 내에 모든 스레드가 완료되지 않았습니다 (데드락 의심)")
                .isTrue()

        }.doesNotThrowAnyException()

        executor.shutdown()

        // Then — 버퍼 잔량 정합성: 생산된 수 - 소비된 수 = 잔여 수
        assertThat(buffer.size())
            .withFailMessage(
                "버퍼 잔량 불일치: produced=${producedCount.get()}, " +
                "consumed=${consumedCount.get()}, buffer=${buffer.size()}"
            )
            .isEqualTo(producedCount.get() - consumedCount.get())
    }
}
