package com.rarible.core.test.containers

import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.WaitingConsumer
import org.testcontainers.utility.DockerImageName

abstract class BaseTestContainersReplicaSetMongoTest {

    companion object {
        const val MONGODB_EXPOSED_PORT = 27017
        val MONGO_IMAGE: DockerImageName = DockerImageName.parse("mongo:4.0.21")

        private val network: Network = Network.newNetwork()

        @JvmStatic
        val mongo: MongoDBContainer by lazy {
            MongoDBContainer(MONGO_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("M1")
                .withExposedPorts(MONGODB_EXPOSED_PORT)
                .withCommand("--replSet rs0 --bind_ip localhost,M1")
        }

        init {
            mongo.start()

            mongo.execInContainer(
                "/bin/bash",
                "-c",
                "mongo --eval 'printjson(rs.initiate({_id:\"rs0\"," +
                        "members:[{_id:0,host:\"M1:27017\"}]}))' --quiet"
            )
            val consumer = WaitingConsumer()
            mongo.followOutput(consumer, OutputFrame.OutputType.STDOUT)
            consumer.waitUntil {
                it.utf8String.contains("transition to primary complete; database writes are now permitted")
            }
        }
    }
}