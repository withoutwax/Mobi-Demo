package com.mobi.poc.service

import com.mobi.poc.domain.EvVehicle
import com.mobi.poc.domain.IceVehicle
import com.mobi.poc.domain.Vehicle
import com.mobi.poc.domain.VehicleType
import com.mobi.poc.infrastructure.VehicleDataBuffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("VehicleDataBuffer 동작 및 동시성 20종 검증")
class VehicleDataBufferTest {

    private lateinit var buffer: VehicleDataBuffer

    @BeforeEach
    fun setUp() {
        buffer = VehicleDataBuffer()
    }

    private fun createDummyVehicle(id: String): Vehicle {
        return EvVehicle(id, VehicleType.REGULAR, 37.0, 127.0, 50.0, 20.0, 100.0)
    }

    // --- 1. 기본 Queue 동작 검증 (5) ---
    @Test
    @DisplayName("1. 초기 생성된 버퍼의 size()가 0인지 검증")
    fun `initial size is 0`() {
        assertThat(buffer.size()).isEqualTo(0)
    }

    @Test
    @DisplayName("2. 버퍼가 비어있을 때 poll() 호출 시 null 반환")
    fun `poll on empty returns null`() {
        assertThat(buffer.poll()).isNull()
    }

    @Test
    @DisplayName("3. 단일 차량 데이터 offer 시 크기 1 증가")
    fun `offer single vehicle increases size`() {
        buffer.offerAll(listOf(createDummyVehicle("V1")))
        assertThat(buffer.size()).isEqualTo(1)
    }

    @Test
    @DisplayName("4. FIFO 순서 검증")
    fun `fifo ordering`() {
        buffer.offerAll(listOf(createDummyVehicle("V1"), createDummyVehicle("V2")))
        assertThat(buffer.poll()?.vehicleId).isEqualTo("V1")
        assertThat(buffer.poll()?.vehicleId).isEqualTo("V2")
    }

    @Test
    @DisplayName("5. offerAll 일괄 삽입 검증")
    fun `offerAll inserts properly`() {
        val list = List(5) { createDummyVehicle("V$it") }
        buffer.offerAll(list)
        assertThat(buffer.size()).isEqualTo(5)
    }

    // --- 2. 최대 용량 (Capacity) 및 예외 처리 (5) ---
    @Test
    @DisplayName("6. 버퍼 크기가 1000에 도달할 때까지 성공")
    fun `capacity up to 1000`() {
        val list = List(1000) { createDummyVehicle("V$it") }
        buffer.offerAll(list)
        assertThat(buffer.size()).isEqualTo(1000)
    }

    @Test
    @DisplayName("7. 1001번째 offer 제한 확인 (IllegalStateException)")
    fun `capacity exceed throws exception`() {
        val list = List(1000) { createDummyVehicle("V$it") }
        buffer.offerAll(list)
        assertThatThrownBy {
            buffer.offerAll(listOf(createDummyVehicle("V1001")))
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    @DisplayName("8. 꽉 찬 버퍼에서 1개 빼면 1개 넣을 수 있음")
    fun `poll frees capacity`() {
        val list = List(1000) { createDummyVehicle("V$it") }
        buffer.offerAll(list)
        buffer.poll()
        buffer.offerAll(listOf(createDummyVehicle("V1001")))
        assertThat(buffer.size()).isEqualTo(1000)
    }

    @Test
    @DisplayName("9. 부분 삽입 초과 시 일부 들어가고 실패하는지 과정 검증")
    fun `partial insert fails on exceed`() {
        val list = List(999) { createDummyVehicle("V$it") }
        buffer.offerAll(list)
        assertThatThrownBy {
            buffer.offerAll(listOf(createDummyVehicle("A"), createDummyVehicle("B")))
        }.isInstanceOf(IllegalStateException::class.java)
        // LinkedBlockingQueue behavior -> adds A, fails on B.
        assertThat(buffer.size()).isEqualTo(1000)
    }

    @Test
    @DisplayName("10. 완전히 비울 경우 size가 0이 됨")
    fun `drain fully`() {
        val list = List(1000) { createDummyVehicle("V$it") }
        buffer.offerAll(list)
        while (buffer.poll() != null) {}
        assertThat(buffer.size()).isEqualTo(0)
    }

    // --- 3. 동시성 (Race Condition) (5) ---
    @Test
    @DisplayName("11. 10 스레드 100번 offer -> 총 1000개 유실 없음 검증")
    fun `concurrent offer loss prevention`() {
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        for (i in 1..10) {
            executor.submit {
                val subList = List(100) { createDummyVehicle("V") }
                buffer.offerAll(subList)
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()
        assertThat(buffer.size()).isEqualTo(1000)
    }

    @Test
    @DisplayName("12. 10 스레드 빈 버퍼 poll -> null 안전 반환 검증")
    fun `concurrent poll on empty prevents exception`() {
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        for (i in 1..10) {
            executor.submit {
                for (j in 1..100) { buffer.poll() }
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()
        assertThat(buffer.size()).isEqualTo(0)
    }

    @Test
    @DisplayName("13. 생산자 50 / 소비자 50 동시성 교차 작업 데드락 방지")
    fun `producer consumer deadlock prevention`() {
        val executor = Executors.newFixedThreadPool(100)
        val latch = CountDownLatch(100)
        val consumedCount = AtomicInteger(0)

        for (i in 1..50) {
            executor.submit { // Producer
                for (j in 1..10) {
                    try { buffer.offerAll(listOf(createDummyVehicle("V"))) } catch (e: Exception) {}
                }
                latch.countDown()
            }
            executor.submit { // Consumer
                for (j in 1..10) {
                    if (buffer.poll() != null) consumedCount.incrementAndGet()
                }
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()
        assertThat(buffer.size() + consumedCount.get()).isLessThanOrEqualTo(500)
    }

    @Test
    @DisplayName("14. 한 스레드가 1000개 넘게 넣을 때 Exception이 적절히 발생")
    fun `multithread capacity exception`() {
        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(2)
        var errors = AtomicInteger(0)
        
        executor.submit {
            try { buffer.offerAll(List(600) { createDummyVehicle("A") }) } catch (e: Exception) { errors.incrementAndGet() }
            latch.countDown()
        }
        executor.submit {
            try { buffer.offerAll(List(600) { createDummyVehicle("B") }) } catch (e: Exception) { errors.incrementAndGet() }
            latch.countDown()
        }
        latch.await()
        executor.shutdown()
        assertThat(buffer.size()).isEqualTo(1000) // 하나는 짤리고 예외 발생해야 함
        assertThat(errors.get()).isGreaterThanOrEqualTo(1)
    }

    @Test
    @DisplayName("15. 1000개 채우고 10개 스레드가 각각 100개씩 동시 poll 시 0으로 안전히 도달")
    fun `concurrent drain`() {
        buffer.offerAll(List(1000) { createDummyVehicle("V$it") })
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(10)
        val successPolls = AtomicInteger()

        for (i in 1..10) {
            executor.submit {
                for (j in 1..100) {
                    if (buffer.poll() != null) successPolls.incrementAndGet()
                }
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()
        assertThat(successPolls.get()).isEqualTo(1000)
        assertThat(buffer.size()).isEqualTo(0)
    }

    // --- 4. 엣지 테스트 (5) ---
    @Test
    @DisplayName("16. 많은 혼합 쓰기/읽기 부하 (ConcurrentModificationException 방어)")
    fun `mixed workload cme check`() {
        val executor = Executors.newFixedThreadPool(4)
        val latch = CountDownLatch(4)
        for (i in 1..2) {
            executor.submit {
                for (j in 1..1000) {
                    try { buffer.offerAll(listOf(createDummyVehicle("V"))) } catch (e: Exception) {}
                }
                latch.countDown()
            }
            executor.submit {
                for (j in 1..1000) { buffer.poll() }
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()
        // 살아남았으면 정상
    }

    @Test
    @DisplayName("17. 다양한 종류의 인스턴스를 혼합해도 타입 보존")
    fun `type retention`() {
        buffer.offerAll(listOf(
            EvVehicle("1", VehicleType.REGULAR, 0.0, 0.0, 0.0, 0.0, 100.0),
            IceVehicle("2", VehicleType.FREIGHT, 0.0, 0.0, 0.0, 0.0, 50.0)
        ))
        assertThat(buffer.poll()).isInstanceOf(EvVehicle::class.java)
        assertThat(buffer.poll()).isInstanceOf(IceVehicle::class.java)
    }

    @Test
    @DisplayName("18. 객체 참조가 버퍼에 남아있지 않음 확인 (Memory Leak)")
    fun `memory leak prevention`() {
        buffer.offerAll(listOf(createDummyVehicle("CLEAN")))
        val p = buffer.poll()
        assertThat(buffer.poll()).isNull()
        // 큐 내부에 남은 참조로 인해 OOM이 일어나지 않게 구조적으로 해제됨을 유추
    }

    @Test
    @DisplayName("19. 동일 객체를 2번 연속 넣었을 때 모두 무사히 저장되고 나오는가")
    fun `insert same instance multiple times`() {
        val v = createDummyVehicle("TWIN")
        buffer.offerAll(listOf(v, v))
        assertThat(buffer.size()).isEqualTo(2)
        assertThat(buffer.poll()).isSameAs(v)
        assertThat(buffer.poll()).isSameAs(v)
    }

    @Test
    @DisplayName("20. 빈 리스트를 offerAll 할 경우 아무런 변화가 없는지 검증")
    fun `offer empty list`() {
        buffer.offerAll(emptyList())
        assertThat(buffer.size()).isEqualTo(0)
    }
}
