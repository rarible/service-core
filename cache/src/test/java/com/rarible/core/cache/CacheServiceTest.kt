package com.rarible.core.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import reactor.core.publisher.Mono
import kotlin.random.Random

@EnableAutoConfiguration
class CacheServiceTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var cacheService: CacheService

    @Test
    fun cached() {
        val descriptor = object : CacheDescriptor<SaveObject> {
            override val collection: String = "test"
            override fun getMaxAge(value: SaveObject?): Long = 100000
            override fun get(id: String): Mono<SaveObject> {
                return Mono.just(SaveObject("value", Random.nextInt()))
            }
        }
        val descriptor2 = object : CacheDescriptor<SaveObject> {
            override val collection: String = "test"
            override fun getMaxAge(value: SaveObject?): Long = 0
            override fun get(id: String): Mono<SaveObject> {
                return Mono.just(SaveObject("value", Random.nextInt()))
            }
        }

        val first = cacheService.getCached("id1", descriptor).block()!!
        assertEquals(first, cacheService.getCached("id1", descriptor).block()!!)
        assertNotEquals(first, cacheService.getCached("id2", descriptor).block()!!)
        val second = cacheService.getCached("id1", descriptor2).block()!!
        assertEquals(first, second)
        Thread.sleep(100)
        val third = cacheService.getCached("id1", descriptor).block()!!
        assertNotEquals(second, third)
        assertEquals(third, cacheService.getCached("id1", descriptor).block()!!)
    }

    @Test
    fun cachedJson() {
        val descriptor = object : CacheDescriptor<JsonObject> {
            override val collection: String = "json"
            override fun getMaxAge(value: JsonObject?): Long = 100000
            override fun get(id: String): Mono<JsonObject> {
                return Mono.just(JsonObject("test", "go"))
            }
        }

        val first = cacheService.getCached("id1", descriptor).block()!!
        println(first)
        val read = cacheService.getCached("id1", descriptor).block()!!
        println(read)
    }

    @Test
    fun `reset key`() {
        val descriptor = object : CacheDescriptor<SaveObject> {
            override val collection: String = "test"
            override fun getMaxAge(value: SaveObject?): Long = 100000
            override fun get(id: String): Mono<SaveObject> =
                Mono.just(SaveObject("value", Random.nextInt()))
        }
        val first = cacheService.getCached("id", descriptor).block()!!
        cacheService.reset("id", descriptor).block()
        val second = cacheService.getCached("id", descriptor).block()!!
        assertNotEquals(first, second)
    }

    private data class SaveObject(
        val filed1: String,
        val filed2: Int
    )

    private data class JsonObject(
        val filed1: String,
        val filed2: String
    )
}