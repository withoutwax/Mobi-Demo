package com.mobi.poc.service

import com.mobi.poc.domain.Vehicle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [Spec 1.3] Real-time Streaming (SSE) 브로드캐스팅 서비스
 *
 * 목적:
 * - 클라이언트의 SSE 구독(subscribe) 요청을 수신 및 관리
 * - 생산된 가상 주행 데이터(Vehicle)를 연결된 모든 클라이언트에게 실시간 단방향 브로드캐스팅
 */
@Service
class SseEmitterService {

    private val log = LoggerFactory.getLogger(javaClass)

    // 스레드 안전한 연결 리스트. 순회 작업(브로드캐스팅)이 빈번하게 발생하므로 CopyOnWriteArrayList가 적합함.
    private val emitters = CopyOnWriteArrayList<SseEmitter>()

    companion object {
        // 커넥션 타임아웃을 60초(1분)로 설정
        private const val DEFAULT_TIMEOUT = 60_000L
    }

    /**
     * 클라이언트 커넥션을 맺고 SseEmitter를 반환한다.
     */
    fun subscribe(): SseEmitter {
        val emitter = SseEmitter(DEFAULT_TIMEOUT)
        
        // 커넥션 목록에 등록
        emitters.add(emitter)

        // 클라이언트 연결 종료/타임아웃/에러 시 메모리 누수 방지를 위해 안전하게 목록에서 제거
        emitter.onCompletion {
            log.debug("SSE Emitter completed.")
            emitters.remove(emitter)
        }
        emitter.onTimeout {
            log.debug("SSE Emitter timeout.")
            emitter.complete()
            emitters.remove(emitter)
        }
        emitter.onError { e ->
            log.debug("SSE Emitter error: {}", e.message)
            emitter.completeWithError(e)
            emitters.remove(emitter)
        }

        // 초기 연결 성공 시 헤더만 날아가게 더미 이벤트 1개 발송 권장 (CORS 관련 timeout 503 방지)
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected successfully"))
        } catch (e: IOException) {
            log.warn("Failed to send init event. Cleaning up emitter.")
            emitters.remove(emitter)
        }

        return emitter
    }

    /**
     * 구독된 모든 클라이언트에게 단방향 이벤트 데이터(Vehicle list)를 전송한다.
     *
     * @param vehicles 브로드캐스팅할 차량 데이터 목록
     */
    fun broadcast(vehicles: List<Vehicle>) {
        if (emitters.isEmpty()) return

        // CopyOnWriteArrayList의 특성상 순회 도중 리스트 구조가 변경되어도
        // ConcurrentModificationException이 발생하지 않음 (스냅샷 기반 순회)
        emitters.forEach { emitter ->
            try {
                // 'vehicles' 이벤트를 JSON 직렬화(전제)하여 클라이언트로 전송
                val event = SseEmitter.event()
                    .name("vehicles")
                    .data(vehicles)
                
                emitter.send(event)
            } catch (e: Exception) { // IOException 포함 포괄적 에러 캐치
                // 전송 중 에러(클라이언트 일방적 종료 등) 발생 시 죽은 Emitter로 간주하고 삭제
                log.warn("Error sending data to an emitter (client likely dead): ${e.message}")
                // 에러 발생한 emitter는 제거하여 서버 리소스 누수 및 불필요한 반복 전송 차단
                emitters.remove(emitter)
            }
        }
    }

    /**
     * 동시성 테스트(SseEmitterServiceTest)에서 Mock Emitter 스텁을 강제 주입하기 위한 메서드.
     */
    fun addEmitterForTest(emitter: SseEmitter) {
        emitters.add(emitter)
    }

    /**
     * 동시성 테스트(SseEmitterServiceTest)에서 접속 중인 Client 수를 검증하기 위한 메서드.
     */
    fun getEmitterCount(): Int {
        return emitters.size
    }
}
