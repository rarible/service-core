package com.rarible.core.test.mongo

import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(AsyncMongoHelper::class)
@Inherited
@ExtendWith(MongoCleanupExtension::class)
annotation class MongoCleanup

class MongoCleanupExtension : BeforeEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        SpringExtension.getApplicationContext(context)
            .autowireCapableBeanFactory.getBean<AsyncMongoHelper>()
            .cleanup()
            .then()
            .block()
    }
}