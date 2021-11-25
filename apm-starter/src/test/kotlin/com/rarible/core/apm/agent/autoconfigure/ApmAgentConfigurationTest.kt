package com.rarible.core.apm.agent.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootTest(
    properties = [
        "rarible.core.apm.agent.enabled=true",
        "rarible.core.apm.agent.packages=com.rarible.core.apm.agent.autoconfigure"
    ]
)
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(ApmAgentConfigurationTest.Configuration::class)
@Disabled // TODO breaks SpanAnnotationPostProcessorTest
class ApmAgentConfigurationTest {

    @Test
    fun `test apm filter initialized`() {

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
