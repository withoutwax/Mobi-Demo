package com.mobi.poc.service

import com.mobi.poc.domain.EvVehicle
import com.mobi.poc.domain.VehicleType
import com.mobi.poc.infrastructure.VehicleDataBuffer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [Spec 1.3] VehicleDataBroadcaster 단위 테스트 (TDD - Red Phase)
 *
 * 검증 대상:
 * - 접속자 유무에 따른 최적화 (접속자 0명 시 버퍼에서 poll 하지 않고 broadcast 생략 여부)
 * - 접속자가 있을 때 버퍼의 데이터를 모두 꺼내어 broadcast()에 올바르게 전달하는지 여부
 * - 전송 중 RuntimeException 예외 발생 시 스케줄러 스레드가 죽지 않는지 (Fault Tolerance)
 */
@DisplayName("VehicleDataBroadcaster 동작 검증")
class VehicleDataBroadcasterTest {

    private lateinit var buffer: VehicleDataBuffer
    private lateinit var sseService: SseEmitterService
    private lateinit var broadcaster: VehicleDataBroadcaster

    @BeforeEach
    fun setUp() {
        buffer = mockk(relaxed = true)
        sseService = mockk(relaxed = true)
        broadcaster = VehicleDataBroadcaster(buffer, sseService)
    }

    @Nested
    @DisplayName("1. 접속자가 없을 때의 최적화 검증")
    inner class OptimizationTest {

        @Test
        @DisplayName("접속중인 클라이언트가 0명이면 broadcast()를 단 한 번도 호출하지 않는다")
        fun `클라이언트 0명 시 broadcast 방어 로직`() {
            // Given
            every { sseService.getEmitterCount() } returns 0

            // When
            broadcaster.broadcastData()

            // Then
            verify(exactly = 0) { sseService.broadcast(any()) }
            // 버퍼에서 데이터를 무의미하게 꺼내는 행위도 막아야 함
            verify(exactly = 0) { buffer.poll() }
        }
    }

    @Nested
    @DisplayName("2. 접속자가 있을 때의 전송 검증")
    inner class TransmissionTest {

        @Test
        @DisplayName("접속자가 최소 1명 이상이면 버퍼에서 데이터를 꺼내와 broadcast()로 전달한다")
        fun `접속자가 있을 때 버퍼 poll 후 broadcast 검증`() {
            // Given
            every { sseService.getEmitterCount() } returns 1

            val dummyVehicle = EvVehicle(
                vehicleId = "TEST-BROAD-001",
                vehicleType = VehicleType.REGULAR,
                latitude = 37.0,
                longitude = 127.0,
                speed = 50.0,
                temperature = 20.0,
                batteryLevel = 100.0
            )

            // mock buffer.poll() 동작: 첫 호출 시 dummyVehicle 반환, 두 번째 호출 시 null (버퍼 바닥남)
            every { buffer.poll() } returns dummyVehicle andThen null

            // When
            broadcaster.broadcastData()

            // Then
            // broadcaster 내부에서 buffer.poll()을 통해 꺼내온 데이터를 리스트로 묶어 전달하는지 검증
            verify(exactly = 1) { sseService.broadcast(listOf(dummyVehicle)) }
        }
    }

    @Nested
    @DisplayName("3. 예외 처리(Fault Tolerance) 검증")
    inner class ResilienceTest {

        @Test
        @DisplayName("데이터 브로드캐스팅 중 런타임 예외가 터져도 스케줄러는 죽지 않고 예외를 잡는다")
        fun `브로드캐스트 중 예외 발생 시 스케줄러 스레드 생존 보장`() {
            // Given
            every { sseService.getEmitterCount() } returns 1
            every { buffer.poll() } returns mockk() andThen null // 최소 1개는 꺼내옴

            // sseService.broadcast 호출 시 강제로 예기치 않은 런타임 예외 투척
            every { sseService.broadcast(any()) } throws RuntimeException("Unexpected Broadcast Error")

            // When & Then
            assertThatCode {
                broadcaster.broadcastData()
            }.doesNotThrowAnyException() // 스레드 밖으로 예외가 전파되지 않아야 함 (catch 작동)

            // 호출 시도는 이뤄졌음을 확인
            verify(exactly = 1) { sseService.broadcast(any()) }
        }
    }
}
