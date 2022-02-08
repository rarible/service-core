package com.rarible.loader.cache

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.configuration.LoadProperties
import com.rarible.core.loader.internal.runner.RetryTasksService
import com.rarible.core.test.containers.KafkaTestContainer
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.loader.cache.configuration.EnableRaribleCacheLoader
import com.rarible.loader.cache.test.TestImage
import com.rarible.loader.cache.test.cacheEvents
import com.rarible.loader.cache.test.testCacheType
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.test.context.ContextConfiguration
import java.time.Clock

@MongoTest
@MongoCleanup
@SpringBootTest(
    properties = []
)
@ContextConfiguration(classes = [TestContext::class])
abstract class AbstractIntegrationTest {
    companion object {
        val kafkaTestContainer = KafkaTestContainer()
    }

    init {
        System.setProperty("kafka.hosts", kafkaTestContainer.kafkaBoostrapServers())
    }

    @Autowired
    lateinit var mongo: ReactiveMongoOperations

    @Autowired
    @Qualifier("test.loader.service")
    lateinit var imageLoadService: CacheLoaderService<TestImage>

    @Autowired
    lateinit var loadProperties: LoadProperties

    @Autowired
    lateinit var retryTasksService: RetryTasksService

    @Autowired
    lateinit var imageLoader: CacheLoader<TestImage>

    @Autowired
    @Qualifier(value = "test.clock")
    lateinit var clock: Clock

    @BeforeEach
    fun clear() {
        clearMocks(clock)
        every { clock.instant() } answers { nowMillis() }

        clearMocks(imageLoader)
        every { imageLoader.type } returns testCacheType
        cacheEvents.clear()
    }
}

@EnableAutoConfiguration
@EnableRaribleCacheLoader
class TestContext {
    @Bean
    @Qualifier("test.loader")
    fun testLoader(): CacheLoader<TestImage> = mockk {
        every { type } returns testCacheType
    }

    @Bean
    @Qualifier("test.loader.service")
    fun imageLoaderService(cacheLoaderServices: List<CacheLoaderService<*>>): CacheLoaderService<TestImage> =
        @Suppress("UNCHECKED_CAST") (cacheLoaderServices.find { it.type == testCacheType } as CacheLoaderService<TestImage>)

    @Bean
    @Qualifier("test.clock")
    @Primary
    fun testClock(): Clock = mockk {
        every { instant() } answers { nowMillis() }
    }

    @Bean
    fun testMeterRegistry(): MeterRegistry = SimpleMeterRegistry()
}
