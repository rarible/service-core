package com.rarible.core.test.containers

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

abstract class MongodbReactiveBaseTest {
    private val mongoContainer = MongodbTestContainer()

    protected fun connectionString(): String {
        return mongoContainer.connectionString()
    }

    protected fun createMongoClient(): MongoClient {
        return MongoClients.create(mongoContainer.connectionString())
    }

    protected fun createReactiveMongoTemplate(database: String? = null): ReactiveMongoTemplate {
        return ReactiveMongoTemplate(createMongoClient(), database ?: "test")
    }
}
