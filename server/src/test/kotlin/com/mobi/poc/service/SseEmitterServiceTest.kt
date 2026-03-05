package com.mobi.poc.service

import com.mobi.poc.domain.EvVehicle
import com.mobi.poc.domain.VehicleType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

/**
 * [Spec 1.3] SseEmitterService 단위 테스트 (TDD - Red Phase)
 *
 * 검증 대상:
 * - 클라이언트 subscribe() 시 유효한 SseEmitter 반환 여부
 * - broadcast() 호출 시 연결된 모든 클라이언트에게 데이터 전송 여부
 * - 전송 중 에러(IOException/Timeout) 발생 시 내부 관리 리스트에서 해당 클라이언트 정상 제거 여부
 */
@DisplayName("SseEmitterService 동작 검증")
class SseEmitterServiceTest {

    private lateinit var sseService: SseEmitterService

    @BeforeEach
    fun setUp() {
        sseService = SseEmitterService()
    }

    @Nested
    @DisplayName("1. 클라이언트 구독 (Subscribe)")
    inner class SubscribeTest {
        @Test
        @DisplayName("subscribe() 호출 시 SseEmitter를 반환하고, 내부 목록에 등록된다")
        fun `subscribe returns valid SseEmitter and registers it`() {
            // When
            val emitter = sseService.subscribe()

            // Then
            assertThat(emitter).isNotNull
            assertThat(sseService.getEmitterCount()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("2. 데이터 브로드캐스팅 (Broadcast)")
    inner class BroadcastTest {
        @Test
        @DisplayName("차량 데이터 리스트가 주어지면, 등록된 모든 SseEmitter를 통해 데이터를 전송한다")
        fun `broadcast sends data to all registered emitters`() {
            // Given - 실제 subscribe 대신 Mock Emitter 2개를 내부 목록에 직접 주입(리플렉션/테스트 전용 메서드 가정) 시뮬레이션
            // 테스트 용이를 위해 SseEmitter 자체를 상속/Mocking 하거나, Service 측에 Mock을 추가할 수 있는 구조 필요
            // 여기서는 Service가 addEmitter(SseEmitter) 같은 메서드를 제공하여 Mock 주입이 가능하다고 가정합니다.
            val mockEmitter1 = mockk<SseEmitter>(relaxed = true)
            val mockEmitter2 = mockk<SseEmitter>(relaxed = true)

            sseService.addEmitterForTest(mockEmitter1)
            sseService.addEmitterForTest(mockEmitter2)

            val dummyVehicles = listOf(
                EvVehicle(
                    vehicleId = "TEST-EV-001",
                    vehicleType = VehicleType.REGULAR,
                    latitude = 37.0,
                    longitude = 127.0,
                    speed = 50.0,
                    temperature = 20.0,
                    batteryLevel = 100.0
                )
            )

            // When
            sseService.broadcast(dummyVehicles)

            // Then
            // 각 Emitter의 send() 메서드가 1번씩 호출되었는지 검증
            verify(exactly = 1) { mockEmitter1.send(any<SseEmitter.SseEventBuilder>()) }
            verify(exactly = 1) { mockEmitter2.send(any<SseEmitter.SseEventBuilder>()) }
        }
    }

    @Nested
    @DisplayName("3. 연결 정리 및 에러 핸들링 (Cleanup)")
    inner class CleanupTest {
        @Test
        @DisplayName("브로드캐스팅 중 IOException이 발생한 Emitter는 즉시 내부 관리 목록에서 제거된다")
        fun `dead emitters are removed on IOException during broadcast`() {
            // Given
            val healthyEmitter = mockk<SseEmitter>(relaxed = true)
            val deadEmitter = mockk<SseEmitter>(relaxed = true)

            // deadEmitter는 send 호출 시 IOException을 발생시키도록 설정
            every { deadEmitter.send(any<SseEmitter.SseEventBuilder>()) } throws IOException("Client disconnected abruptly")

            sseService.addEmitterForTest(healthyEmitter)
            sseService.addEmitterForTest(deadEmitter)

            assertThat(sseService.getEmitterCount()).isEqualTo(2)

            val dummyVehicles = emptyList<EvVehicle>()

            // When
            sseService.broadcast(dummyVehicles)

            // Then
            // 예외가 서버를 죽이지 않아야 함 (broadcast에서 catch 처리됨)
            // healthyEmitter는 여전히 남아있고, deadEmitter는 제거되어 count가 1이 되어야 함
            assertThat(sseService.getEmitterCount()).isEqualTo(1)
            
            // healthyEmitter는 정상적으로 전송을 시도했는지 검증
            verify(exactly = 1) { healthyEmitter.send(any<SseEmitter.SseEventBuilder>()) }
        }
    }
}
