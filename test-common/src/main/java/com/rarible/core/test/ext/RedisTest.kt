package com.rarible.core.test.ext

import com.rarible.core.test.containers.RedisTestContainer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(RedisTestExtension::class)
annotation class RedisTest

class RedisTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        System.setProperty(
            "redisUri", redis.redisUrl()
        )
    }

    companion object {
        val redis = RedisTestContainer()
    }
}
