package com.rarible.core.lockredis

import com.rarible.core.test.containers.RedisTestContainer

abstract class BaseRedisTest {
    init {
        System.setProperty(
            "redisUri", redis.redisUrl()
        )
        System.setProperty(
            "spring.data.mongodb.database", "rarible-core"
        )
    }
    companion object {
        val redis = RedisTestContainer()
    }
}
