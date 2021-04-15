package com.rarible.core.task

import com.rarible.core.mongo.configuration.IncludePersistProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableAutoConfiguration
@EnableRaribleTask
@IncludePersistProperties
class MockContext {
    @Bean
    @Qualifier("mockHandler1")
    fun mockHandler1() = MockHandler("MOCK1")

    @Bean
    @Qualifier("mockHandler2")
    fun mockHandler2() = MockHandler("MOCK2")
}