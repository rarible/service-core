package com.rarible.core.apm.annotation

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Span
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
import kotlinx.coroutines.runBlocking
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
    private lateinit var transactionClass: TransactionClass

    @Test
    fun `should handle mono transaction annotation`() {
        mockkStatic(ElasticApm::class)
        val transaction = mockk<Transaction>()
        every { transaction.setName("testTransaction") } returns transaction
        every { transaction.end() } returns Unit

        every { ElasticApm.startTransaction() } returns transaction

        transactionClass.openMonoTransaction().block()

        verify(exactly = 1) { ElasticApm.startTransaction() }
        verify(exactly = 1) { transaction.setName("testTransaction") }
        verify(exactly = 1) { transaction.end() }
    }

    @Test
    fun `should handle suspend transaction annotation`() = runBlocking<Unit> {
        mockkStatic(ElasticApm::class)
        val transaction = mockk<Transaction>()
        every { transaction.setName("testTransaction") } returns transaction
        every { transaction.end() } returns Unit

        every { ElasticApm.startTransaction() } returns transaction

        transactionClass.openSuspendTransaction()

        verify(exactly = 1) { ElasticApm.startTransaction() }
        verify(exactly = 1) { transaction.setName("testTransaction") }
        verify(exactly = 1) { transaction.end() }
    }

    @Test
    fun `should handle transaction and span annotation`() {
        mockkStatic(ElasticApm::class)
        val transaction = mockk<Transaction>()
        val span = mockk<Span>()

        every { transaction.setName("testTransaction") } returns transaction
        every { transaction.end() } returns Unit
        every { transaction.startSpan("testType", "testSubType", "testAction") } returns span

        every { span.setName("testName") } returns span
        every { span.end() } returns Unit

        every { ElasticApm.startTransaction() } returns transaction

        transactionClass.openMonoTransactionAndSpan().block()

        verify(exactly = 1) { ElasticApm.startTransaction() }
        verify(exactly = 1) { transaction.setName("testTransaction") }
        verify(exactly = 1) { transaction.startSpan("testType", "testSubType", "testAction") }
        verify(exactly = 1) { transaction.end() }
        verify(exactly = 1) { span.setName("testName") }
        verify(exactly = 1) { span.end() }
    }

    @TestConfiguration
    internal class Configuration {
        @Bean
        fun spanClass(): SpanClass {
            return SpanClass()
        }

        @Bean
        fun transactionClass(monoSpanClass: SpanClass): TransactionClass {
            return TransactionClass(monoSpanClass)
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
    open class TransactionClass(
        private val spanClass: SpanClass
    ) {
        @CaptureTransaction("testTransaction")
        open fun openMonoTransaction(): Mono<String> {
            return Mono.just("Open Transaction")
        }

        @CaptureTransaction("testTransaction")
        open fun openMonoTransactionAndSpan(): Mono<String> {
            return Mono
                .just("Open Transaction")
                .then(spanClass.openMonoSpan())
        }

        @CaptureTransaction("testTransaction")
        open suspend fun openSuspendTransaction(): String {
            return "Open Transaction"
        }

        @CaptureTransaction("testTransaction")
        open suspend fun openSuspendTransactionAndSpan(): String {
            return spanClass.openSuspendSpan()
        }
    }

    @Spanable
    open class SpanClass {
        @CaptureSpan(value = "testName", type = "testType", subtype = "testSubType", action = "testAction")
        open fun openMonoSpan(): Mono<String> {
            return Mono.just("Open Span")
        }

        @CaptureSpan(value = "testName", type = "testType", subtype = "testSubType", action = "testAction")
        open suspend fun openSuspendSpan(): String {
            return "Open Span"
        }
    }
}
