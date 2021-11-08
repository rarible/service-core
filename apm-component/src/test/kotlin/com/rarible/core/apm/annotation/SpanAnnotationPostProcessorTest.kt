package com.rarible.core.apm.annotation

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Transaction
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.CaptureTransaction
import com.rarible.core.apm.Spanable
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import reactor.core.publisher.Mono

@SpringBootTest(
    properties = [
        "rarible.core.apm.annotation.enabled=true"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(SpanAnnotationPostProcessorTest.Configuration::class)
class SpanAnnotationPostProcessorTest {
    @Autowired
    private lateinit var testAnnotatedClass: TestAnnotatedClass

    @Test
    fun `should handle transaction annotation`() {
        mockkStatic(ElasticApm::class)
        val transaction = mockk<Transaction>()
        every { transaction.setName("testTransaction") } returns transaction
        every { transaction.end() } returns Unit

        every { ElasticApm.startTransaction() } returns transaction

        testAnnotatedClass.openTransaction().block()

        verify(exactly = 1) { ElasticApm.startTransaction() }
        verify(exactly = 1) { transaction.setName("testTransaction") }
        verify(exactly = 1) { transaction.end() }
    }

    @TestConfiguration
    internal class Configuration {
        @Bean
        fun testAnnotatedClass(): TestAnnotatedClass {
            return TestAnnotatedClass()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }

        @Bean
        fun applicationInfo(): ApplicationInfo {
            return ApplicationInfo("test", "test.com")
        }
    }

    @Spanable
    open class TestAnnotatedClass {
        @CaptureTransaction("testTransaction")
        open fun openTransaction(): Mono<String> {
            return Mono.just("Open Transaction")
        }

        @CaptureSpan(value = "testName", type = "testType", subtype = "testSubType", action = "testAction")
        open fun openSpan(): Mono<String> {
            return Mono.just("Open Span")
        }
    }
}
