package com.mobi.poc.controller

import com.mobi.poc.service.SseEmitterService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = ["http://localhost:3000"])
class VehicleController(
    private val sseEmitterService: SseEmitterService
) {

    /**
     * [Spec 1.3] 클라이언트가 실시간 스트리밍 데이터를 받기 위해 연결하는 SSE 엔드포인트
     */
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamVehicles(): SseEmitter {
        return sseEmitterService.subscribe()
    }
}
