package com.rarible.loader.cache

import com.rarible.core.common.nowMillis
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.loader.cache.test.TestImage
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CacheRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var cacheRepository: CacheRepository

    @Test
    fun `empty cache`() = runBlocking<Unit> {
        assertThat(cacheRepository.get<String>("type", "key")).isNull()
    }

    @Test
    fun `save and get`() = runBlocking<Unit> {
        val type = "type"
        val key = "key"
        val testImage = TestImage("data")
        val cachedAt = nowMillis()
        cacheRepository.save(type, key, testImage, cachedAt)
        assertThat(cacheRepository.get<TestImage>(type, key)).isEqualTo(
            MongoCacheEntry(
                key = key,
                data = testImage,
                cachedAt = cachedAt
            )
        )
        assertThat(cacheRepository.contains(type, key)).isTrue
        assertThat(cacheRepository.get<TestImage>("otherType", key)).isNull()
        assertThat(cacheRepository.get<TestImage>(type, "otherKey")).isNull()
    }

    @Test
    fun remove() = runBlocking<Unit> {
        val type = "type"
        val key = "key"
        assertThat(cacheRepository.get<TestImage>(type, key)).isNull()
        val testImage = TestImage("data")
        cacheRepository.save(type, key, testImage, nowMillis())
        assertThat(cacheRepository.get<TestImage>(type, key)).isNotNull
        assertThat(cacheRepository.contains(type, key)).isTrue
        cacheRepository.remove(type, key)
        assertThat(cacheRepository.get<TestImage>(type, key)).isNull()
        assertThat(cacheRepository.contains(type, key)).isFalse()
    }

    @Test
    fun update() = runBlocking<Unit> {
        val type = "type"
        val key = "key"
        assertThat(cacheRepository.get<TestImage>(type, key)).isNull()
        val testImage = TestImage("data")
        val cachedAt = nowMillis()
        cacheRepository.save(type, key, testImage, cachedAt)

        val testImage2 = TestImage("data2")
        val updatedAt = nowMillis()
        cacheRepository.save(type, key, testImage2, updatedAt)
        assertThat(cacheRepository.get<TestImage>(type, key)).isEqualTo(
            MongoCacheEntry(
                key = key,
                data = testImage2,
                cachedAt = updatedAt
            )
        )
    }
}
