package com.rarible.core.apm.web.filter.autoconfigure

import com.rarible.core.apm.web.filter.RequestPerformanceFilter
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootTest(
    properties = [
        "rarible.core.filter.apm.enabled=true"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(RequestPerformanceFilterConfigurationTest.Configuration::class)
class RequestPerformanceFilterConfigurationTest {

    @Autowired
    private lateinit var requestPerformanceFilter: RequestPerformanceFilter

    @Test
    fun `test apm filter initialized`() {
        Assertions.assertThat(requestPerformanceFilter).isNotNull
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
}
