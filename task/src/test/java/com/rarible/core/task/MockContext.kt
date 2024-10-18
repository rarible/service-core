package com.rarible.core.task

import com.rarible.core.mongo.configuration.IncludePersistProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableAutoConfiguration
@EnableRaribleTask
@IncludePersistProperties
class MockContext {
    @Bean
    fun mockHandler() = MockHandler(MOCK_TASK_TYPE)

    @Bean
    fun meterRegistry() = SimpleMeterRegistry()
}

const val MOCK_TASK_TYPE = "MOCK"
