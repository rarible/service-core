package com.rarible.core.autoconfigure.filter.cors

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

import org.springframework.web.reactive.config.CorsRegistry

import org.springframework.web.reactive.config.WebFluxConfigurer

@ConditionalOnProperty(prefix = RARIBLE_FILTER_CORS, name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(CorsWebFilterProperties::class)
class CorsWebFilterAutoConfiguration {

    @Bean
    fun corsGlobalConfiguration(): CorsGlobalConfiguration {
        return CorsGlobalConfiguration()
    }

    class CorsGlobalConfiguration : WebFluxConfigurer {
        override fun addCorsMappings(corsRegistry: CorsRegistry) {
            corsRegistry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("PUT", "GET", "POST", "OPTIONS", "HEAD")
                .maxAge(3600)
        }
    }
}