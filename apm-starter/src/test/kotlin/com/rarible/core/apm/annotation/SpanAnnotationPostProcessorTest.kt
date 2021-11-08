package com.rarible.core.apm.annotation

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Span
import co.elastic.apm.api.Transaction
import com.rarible.core.apm.ApmContext
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.CaptureTransaction
import com.rarible.core.apm.getApmContext
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import reactor.core.publisher.Mono
import reactor.util.context.Context

@SpringBootTest(
    properties = [
        "rarible.core.apm.annotation.enabled=true"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@ExperimentalCoroutinesApi
@Import(SpanAnnotationPostProcessorTest.Configuration::class)
class SpanAnnotationPostProcessorTest {
    @Autowired
    private lateinit var transactionClass: TransactionClass

    @Autowired
    private lateinit var spanClassAnnotated: SpanClassAnnotated

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
    fun `should handle mono transaction and span annotation`() {
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
    fun `should handle suspend transaction and span annotation`() = runBlocking<Unit> {
        mockkStatic(ElasticApm::class)
        val transaction = mockk<Transaction>()
        val span = mockk<Span>()

        every { transaction.setName("testTransaction") } returns transaction
        every { transaction.end() } returns Unit
        every { transaction.startSpan("testType", "testSubType", "testAction") } returns span

        every { span.setName("testName") } returns span
        every { span.end() } returns Unit

        every { ElasticApm.startTransaction() } returns transaction

        transactionClass.openSuspendTransactionAndSpan()

        verify(exactly = 1) { ElasticApm.startTransaction() }
        verify(exactly = 1) { transaction.setName("testTransaction") }
        verify(exactly = 1) { transaction.startSpan("testType", "testSubType", "testAction") }
        verify(exactly = 1) { transaction.end() }
        verify(exactly = 1) { span.setName("testName") }
        verify(exactly = 1) { span.end() }
    }

    @Test
    fun `should handle class annotated span capture method with no extra annotation`() = runBlocking<Unit> {
        mockkStatic(ElasticApm::class)
        val transaction = mockk<Transaction>()
        val span = mockk<Span>()

        every { transaction.startSpan(null, null, null) } returns span

        every { span.setName("db") } returns span
        every { span.end() } returns Unit

        withContext(
            ReactorContext(Context.empty().put(ApmContext.Key, ApmContext(transaction)))
        ) {
            spanClassAnnotated.methodWithoutAnnotation()
        }

        verify(exactly = 1) { transaction.startSpan(null, null, null) }
        verify(exactly = 1) { span.setName("db") }
        verify(exactly = 1) { span.end() }
    }

    @Test
    fun `should handle class annotated span capture method with extra annotation`() = runBlocking<Unit> {
        mockkStatic(ElasticApm::class)
        val transaction = mockk<Transaction>()
        val span = mockk<Span>()

        every { transaction.startSpan("testType", "testSubType", "testAction") } returns span

        every { span.setName("db") } returns span
        every { span.end() } returns Unit

        withContext(
            ReactorContext(Context.empty().put(ApmContext.Key, ApmContext(transaction)))
        ) {
            spanClassAnnotated.methodWithAnnotation()
        }

        verify(exactly = 1) { transaction.startSpan("testType", "testSubType", "testAction") }
        verify(exactly = 1) { span.setName("db") }
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
        fun spanClassAnnotated(): SpanClassAnnotated {
            return SpanClassAnnotated()
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
            delay(1)
            assertThat(getApmContext()).isNotNull
            return "Open Transaction"
        }

        @CaptureTransaction("testTransaction")
        open suspend fun openSuspendTransactionAndSpan(): String {
            delay(1)
            return spanClass.openSuspendSpan()
        }
    }

    open class SpanClass {
        @CaptureSpan(value = "testName", type = "testType", subtype = "testSubType", action = "testAction")
        open fun openMonoSpan(): Mono<String> {
            return Mono.just("Open Span")
        }

        @CaptureSpan(value = "testName", type = "testType", subtype = "testSubType", action = "testAction")
        open suspend fun openSuspendSpan(): String {
            delay(1)
            assertThat(getApmContext()).isNotNull
            return "Open Span"
        }
    }

    @CaptureSpan(value = "db")
    open class SpanClassAnnotated {
        open suspend fun methodWithoutAnnotation() { }

        @CaptureSpan(type = "testType", subtype = "testSubType", action = "testAction")
        open suspend fun methodWithAnnotation() { }
    }
}
