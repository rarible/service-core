package com.rarible.core.test.containers

class RedisTestContainer {

    fun redisUrl(): String {
        return "redis://localhost:${redis.getMappedPort(REDIS_PORT)}"
    }

    companion object {
        const val REDIS_PORT = 6379

        private val redis: KGenericContainer = KGenericContainer("redis:6.0.9-alpine")
            .withExposedPorts(REDIS_PORT)
            .withReuse(true)

        init {
            redis.start()
        }
    }
}