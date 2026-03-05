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
import org.junit.jupiter.api.Test

/**
 * [Spec 1.1] VehicleDataProducer 단위 테스트 (TDD - Red Phase)
 *
 * 검증 대상:
 * - 스케줄러(produceData)가 Generator에서 데이터를 생성받아 Buffer에 정확히 전달하는가 (행위 검증)
 * - 버퍼가 가득 차서 예외가 발생하더라도 스케줄러가 종료되지 않고 예외를 삼키거나 적절히 처리하는가
 */
@DisplayName("VehicleDataProducer 동작 검증")
class VehicleDataProducerTest {

    private lateinit var generator: VehicleDataGenerator
    private lateinit var buffer: VehicleDataBuffer
    private lateinit var producer: VehicleDataProducer

    @BeforeEach
    fun setUp() {
        // MockK를 사용하여 격리된 단위 테스트 환경 구성
        generator = mockk()
        buffer = mockk(relaxed = true) // 반환값이 없는 메서드는 기본 동작 허용
        producer = VehicleDataProducer(generator, buffer)
    }

    @Test
    @DisplayName("produceData()가 호출되면 generator가 1회, buffer의 offerAll()이 1회 호출된다")
    fun `행위 검증 - 데이터 정상 전달`() {
        // Given
        val dummyData = buildList {
            add(
                EvVehicle(
                    vehicleId = "TEST-001",
                    vehicleType = VehicleType.REGULAR,
                    latitude = 37.0,
                    longitude = 127.0,
                    speed = 50.0,
                    temperature = 20.0,
                    batteryLevel = 100.0
                )
            )
        }
        every { generator.generate() } returns dummyData

        // When
        producer.produceData()

        // Then
        // generator.generate()가 정확히 1번 호출되었는지 검증
        verify(exactly = 1) { generator.generate() }
        // buffer.offerAll()에 dummyData가 정확히 1번 인자로 전달되었는지 검증
        verify(exactly = 1) { buffer.offerAll(dummyData) }
    }

    @Test
    @DisplayName("버퍼가 가득 차서 IllegalStateException이 발생하더라도 스케줄러 로직은 예외를 던지지 않고 꿀꺽 삼킨다")
    fun `예외 처리 검증 - 버퍼 초과 발생 시 스케줄러 생존 보장`() {
        // Given
        val dummyData = emptyList<EvVehicle>()
        every { generator.generate() } returns dummyData
        
        // buffer.offerAll() 호출 시 강제로 예외 발생
        every { buffer.offerAll(any()) } throws IllegalStateException("Buffer capacity exceeded")

        // When & Then
        // 예외가 외부로 전파되어 스케줄러 스레드가 죽지 않도록 예외를 잡는지 확인
        assertThatCode {
            producer.produceData()
        }.doesNotThrowAnyException()

        // 그래도 호출은 정상적으로 시도되었어야 함
        verify(exactly = 1) { buffer.offerAll(any()) }
    }
}
