package com.mobi.poc.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "status" to "UP",
            "service" to "mobility-streamer",
            "timestamp" to LocalDateTime.now().toString()
        )
        return ResponseEntity.ok(body)
    }
}
