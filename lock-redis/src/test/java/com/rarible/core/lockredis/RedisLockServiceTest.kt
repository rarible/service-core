package com.rarible.core.lockredis

import com.rarible.core.test.wait.BlockingWait.waitAssert
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink

class RedisLockServiceTest : AbstractIntegrationTest() {
    @Test
    fun lockUnlocks() {
        val key = RandomStringUtils.randomAlphabetic(10)

        val value1 = RandomStringUtils.randomAlphabetic(10)
        val result1 = lockService.synchronize(key, 100000, Mono.just(value1)).block()
        assertEquals(value1, result1)

        val value2 = RandomStringUtils.randomAlphabetic(10)
        val result2 = lockService.synchronize(key, 100000, Mono.just(value2)).block()
        assertEquals(value2, result2)
    }

    @Test
    fun lockWaits() {
        val key = RandomStringUtils.randomAlphabetic(10)

        var sink1: MonoSink<String>? = null
        var result1: String? = null
        val value1 = RandomStringUtils.randomAlphabetic(10)
        lockService.synchronize(key, 100000, Mono.create<String> { sink1 = it }).subscribe { result1 = it }
        while (sink1 == null) {
            Thread.sleep(50)
        }

        val value2 = RandomStringUtils.randomAlphabetic(10)
        var result2: String? = null
        var subscribed = false
        val mono2 = Mono.defer {
            subscribed = true
            Mono.just(value2)
        }
        lockService.synchronize(key, 100000, mono2).subscribe { result2 = it }

        Thread.sleep(1000)
        assertEquals(null, result2)
        assertEquals(false, subscribed)
        sink1?.success(value1)

        waitAssert(600) {
            assertEquals(value1, result1)
            assertEquals(value2, result2)
            assertEquals(true, subscribed)
        }
    }

    @Test
    fun contextWorks() {
        val result = lockService.synchronize("any", 10000, testingCtx)
            .subscriberContext { it.put("testing", "value") }
            .block()

        assertEquals("value", result)
    }

    private val testingCtx: Mono<String> = Mono.subscriberContext().filter { it.hasKey("testing") }.map { it.get<String>("testing") }
}