package com.rarible.core.lockredis

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.reactive.RedisReactiveCommands
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(RedisLockService::class)
class RedisLockConfiguration {
    @Value("\${redisUri:redis://localhost}")
    private lateinit var redisUri: String

    @Bean(destroyMethod = "shutdown")
    fun redisClient(): RedisClient {
        return RedisClient.create(redisUri)
    }

    @Bean(destroyMethod = "close")
    fun redisConnection(): StatefulRedisConnection<String, String> {
        return redisClient().connect()
    }

    @Bean
    fun redisReactive(): RedisReactiveCommands<String, String> {
        return redisConnection().reactive()
    }
}
