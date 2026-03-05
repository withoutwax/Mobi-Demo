package com.mobi.poc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MobilityStreamerApplication

fun main(args: Array<String>) {
    runApplication<MobilityStreamerApplication>(*args)
}
