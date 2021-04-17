package com.rarible.core.telemetry.consul

import com.rarible.core.application.ApplicationEnvironmentInfo
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.info.GitProperties
import org.springframework.cloud.consul.serviceregistry.ConsulRegistrationCustomizer
import org.springframework.context.annotation.Bean

@ConditionalOnProperty(prefix = "spring.cloud.service-registry.auto-registration", name = ["enabled"], havingValue = "true")
class ConsulRegistrationCustomizerAutoConfiguration(
    private val environmentInfo: ApplicationEnvironmentInfo,
    private val gitProperties: GitProperties?
) {
    @Bean
    fun customizer(): ConsulRegistrationCustomizer {
        return ConsulRegistrationCustomizer { consulRegistration ->
            consulRegistration.service.apply {
                val defaultTags = listOfNotNull(
                    "platform=spring-boot",
                    "env=${environmentInfo.name}",
                    gitProperties?.let { "commitHash=${it.commitId}" }
                )
                name = name?.let { "${environmentInfo.name}-$it" }
                tags = (tags ?: emptyList()) + defaultTags
                id = "$id:${environmentInfo.host}"
            }
        }
    }
}