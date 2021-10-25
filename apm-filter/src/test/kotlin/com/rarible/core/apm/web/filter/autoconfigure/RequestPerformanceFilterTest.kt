package com.rarible.core.apm.web.filter.autoconfigure

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Transaction
import com.rarible.core.apm.*
import com.rarible.core.apm.web.filter.RequestPerformanceFilter
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import reactor.core.publisher.Mono

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "rarible.core.filter.apm.enabled=true",
        "rarible.core.apm.annotation.enabled=true"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(RequestPerformanceFilterTest.Configuration::class)
class RequestPerformanceFilterTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var requestPerformanceFilter: RequestPerformanceFilter

    @Autowired
    private lateinit var testAnnotatedClass: TestAnnotatedClass

    @Test
    fun `test apm filter initialized`() {
        assertThat(requestPerformanceFilter).isNotNull
    }

    @Test
    fun `APM context propagated`() {
        val template = RestTemplate()
        val result: Boolean = template.getForObject("http://localhost:${port}/test")
        assertThat(result).isTrue()
    }

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
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }

        @Bean
        fun applicationInfo(): ApplicationInfo {
            return ApplicationInfo("test", "test.com")
        }

        @Bean
        fun testAnnotatedClass(): TestAnnotatedClass {
            return TestAnnotatedClass()
        }
    }

    @RestController
    internal class TestController {
        @GetMapping("/test")
        suspend fun test(): Boolean {
            val ctx = getApmContext()
            return ctx != null
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
