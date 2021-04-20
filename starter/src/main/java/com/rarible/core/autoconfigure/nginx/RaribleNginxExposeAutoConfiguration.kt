package com.rarible.core.autoconfigure.nginx

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.cloud.consul.serviceregistry.ConsulRegistrationCustomizer
import org.springframework.context.annotation.Bean

@ConditionalOnClass(ConsulRegistrationCustomizer::class, ConsulDiscoveryClient::class)
@ConditionalOnProperty(prefix = RARIBLE_CORE_NGINX_EXPOSE, name = ["enabled"], havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(RaribleNginxExposeProperties::class)
class RaribleNginxExposeAutoConfiguration(
    private val properties: RaribleNginxExposeProperties
) {
    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.service-registry.auto-registration", name = ["enabled"], havingValue = "true")
    fun raribleNginxExposeConsulRegistrationCustomizer(): ConsulRegistrationCustomizer {
        return ConsulRegistrationCustomizer { consulRegistration ->
            consulRegistration.service.apply {
                val webApiServer = "web-api:${properties.server}"
                val webApiLocation = "api-location:${properties.location}"
                tags = (tags ?: emptyList()) + listOf(webApiServer, webApiLocation)
            }
        }
    }
}