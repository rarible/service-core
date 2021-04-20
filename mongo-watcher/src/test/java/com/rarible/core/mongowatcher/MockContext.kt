package com.rarible.core.mongowatcher

import com.rarible.core.task.EnableRaribleTask
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@EnableRaribleTask
@Configuration
@EnableAutoConfiguration
class MockContext {
    @Bean
    fun handler(mongo: ReactiveMongoOperations) = MockChangeHandler(mongo)
}