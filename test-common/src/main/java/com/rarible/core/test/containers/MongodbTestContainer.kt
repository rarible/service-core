package com.rarible.core.test.containers

import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

class MongodbTestContainer {

    fun connectionString(): String {
        return "mongodb://${mongo.containerIpAddress}:${mongo.getMappedPort(MONGODB_EXPOSED_PORT)}"
    }

    companion object {
        const val MONGODB_EXPOSED_PORT = 27017
        val MONGO_IMAGE: DockerImageName = DockerImageName.parse("mongo:4.0.21")

        @JvmStatic
        val mongo: MongoDBContainer by lazy {
            MongoDBContainer(MONGO_IMAGE)
                .withExposedPorts(MONGODB_EXPOSED_PORT)
        }

        init {
            mongo.start()
        }
    }
}

