package com.rarible.core.test.containers

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class KafkaTestContainer {
    fun kafkaBoostrapServers(): String {
        return kafka.bootstrapServers
    }

    companion object {
        val KAFKA__IMAGE: DockerImageName = if (System.getProperty("os.arch") == "aarch64")
            DockerImageName.parse("ghcr.io/arm64-compat/confluentinc/cp-kafka:7.1.1").asCompatibleSubstituteFor("confluentinc/cp-kafka")
        else
            DockerImageName.parse("confluentinc/cp-kafka:6.1.1")

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