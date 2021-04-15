package com.rarible.core.mongo

import com.rarible.core.test.containers.MongodbTestContainer

abstract class BaseMongoTest {
    init {
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
