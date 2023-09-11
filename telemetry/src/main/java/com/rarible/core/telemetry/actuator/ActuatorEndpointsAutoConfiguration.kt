package com.rarible.core.telemetry.actuator

import org.springframework.boot.info.GitProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import java.util.Properties

@PropertySource(
    value = ["classpath:/git.properties"],
    ignoreResourceNotFound = true
)
internal class ActuatorEndpointsAutoConfiguration {
    @Bean
    fun statusWebEndpoint() = StatusWebEndpoint()

    @Bean
    fun versionWebEndpoint(gitProperties: GitProperties?): VersionWebEndpoint {
        return VersionWebEndpoint(
            gitProperties = gitProperties ?: GitProperties(Properties())
        )
    }
}
