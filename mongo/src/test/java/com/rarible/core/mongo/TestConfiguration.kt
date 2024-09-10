package com.rarible.core.mongo

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableAutoConfiguration
class TestConfiguration {
    @Bean
    fun meterRegistry(): SimpleMeterRegistry = SimpleMeterRegistry()
}
