package com.rarible.core.test.containers

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

abstract class MongodbReactiveReplicaSetBaseTest : BaseTestContainersReplicaSetMongoTest() {

    protected fun createMongoClient(): MongoClient {
        val connectionString = "mongodb://${mongo.containerIpAddress}:${mongo.getMappedPort(MONGODB_EXPOSED_PORT)}"
        return MongoClients.create(connectionString)
    }

    protected fun createReactiveMongoTemplate(database: String? = null): ReactiveMongoTemplate {
        return createReactiveMongoTemplate(database ?: "test")
    }
}
