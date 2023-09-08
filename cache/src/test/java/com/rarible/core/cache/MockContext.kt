package com.rarible.core.cache

import com.rarible.core.lock.LockService
import com.rarible.core.mongo.configuration.IncludePersistProperties
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
@EnableAutoConfiguration
@EnableRaribleCache
@IncludePersistProperties
class MockContext {
    @Bean
    fun lockService(): LockService {
        return object : LockService {
            override fun <T> synchronize(name: String, expiresMs: Int, op: Mono<T>): Mono<T> {
                return op
            }
        }
    }
}
