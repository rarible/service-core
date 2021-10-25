package com.rarible.core.apm.web.filter.autoconfigure

import com.rarible.core.apm.*
import com.rarible.core.apm.web.filter.RequestPerformanceFilter
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "rarible.core.filter.apm.enabled=true"
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
    }

    @RestController
    internal class TestController {
        @GetMapping("/test")
        suspend fun test(): Boolean {
            val ctx = getApmContext()
            return ctx != null
        }
    }
}
