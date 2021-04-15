package com.rarible.core.test.ext

import com.rarible.core.test.containers.MongodbTestContainer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(MongoTestExtension::class)
annotation class MongoTest

class MongoTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        System.setProperty(
            "spring.data.mongodb.uri", mongoTest.connectionString()
        )
        System.setProperty(
            "spring.data.mongodb.database", "rarible-core"
        )
    }

    companion object {
        val mongoTest = MongodbTestContainer()
    }
}