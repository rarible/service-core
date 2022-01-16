package com.rarible.core.loader

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.configuration.EnableRaribleLoader
import com.rarible.core.loader.configuration.LoadProperties
import com.rarible.core.loader.internal.LoadTaskService
import com.rarible.core.loader.test.testLoaderType
import com.rarible.core.loader.test.testReceivedNotifications
import com.rarible.core.mongo.configuration.IncludePersistProperties
import com.rarible.core.test.containers.KafkaTestContainer
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
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
import org.springframework.context.annotation.Configuration
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
    @Qualifier(value = "test.loader")
    lateinit var loader: Loader

    @Autowired
    @Qualifier(value = "test.clock")
    lateinit var clock: Clock

    @Autowired
    lateinit var loadTaskService: LoadTaskService

    @Autowired
    lateinit var loadProperties: LoadProperties

    @Autowired
    lateinit var mongo: ReactiveMongoOperations

    @BeforeEach
    fun clear() {
        clearMocks(loader, clock)
        every { loader.type } returns testLoaderType
        every { clock.instant() } answers { nowMillis() }
        testReceivedNotifications.clear()
    }
}

@Configuration
@EnableAutoConfiguration
@EnableRaribleLoader
@IncludePersistProperties
class TestContext {
    @Bean
    @Qualifier("test.loader")
    fun testLoader(): Loader = mockk {
        every { type } returns testLoaderType
    }

    @Bean
    @Qualifier("test.clock")
    @Primary
    fun testClock(): Clock = mockk {
        every { instant() } answers { nowMillis() }
    }

    @Bean
    fun testMeterRegistry(): MeterRegistry = SimpleMeterRegistry()
}
