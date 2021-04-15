package com.rarible.core.test.mongo

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(AsyncMongoHelper::class)
@Inherited
@ExtendWith(MongoCleanupExtension::class)
annotation class MongoCleanup

class MongoCleanupExtension : BeforeEachCallback, BeforeAllCallback {

    @Autowired
    private lateinit var mongoHelper: AsyncMongoHelper

    override fun beforeAll(context: ExtensionContext) {
        SpringExtension.getApplicationContext(context)
            .autowireCapableBeanFactory
            .autowireBean(this)
    }

    override fun beforeEach(context: ExtensionContext) {
        mongoHelper.cleanup().blockLast()
    }
}