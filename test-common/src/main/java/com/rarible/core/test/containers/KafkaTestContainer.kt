package com.rarible.core.test.containers

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class KafkaTestContainer {
    fun kafkaBoostrapServers(): String {
        return kafka.bootstrapServers
    }

    companion object {
        val KAFKA__IMAGE: DockerImageName =
            DockerImageName.parse("confluentinc/cp-kafka:7.0.1")

        @JvmStatic
        private val kafka: KafkaContainer by lazy {
            KafkaContainer(KAFKA__IMAGE).apply {
                waitingFor(Wait.defaultWaitStrategy())
                withReuse(true)
            }
        }

        init {
            kafka.start()
        }
    }
}