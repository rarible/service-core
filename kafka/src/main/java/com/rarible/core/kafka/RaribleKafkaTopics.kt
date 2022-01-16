package com.rarible.core.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.slf4j.LoggerFactory
import java.util.*

// TODO: add a utility to cleanup all Kafka topics in a local cluster and an annotation KafkaCleanup (like MongoCleanup)
object RaribleKafkaTopics {

    private val logger = LoggerFactory.getLogger(RaribleKafkaTopics::class.java)

    fun createTopic(
        brokerReplicaSet: String,
        topic: String,
        partitions: Int,
        replicationFactor: Short? = null
    ) {
        useAdminClient(brokerReplicaSet) { adminClient ->
            if (adminClient.listTopics().names().get().contains(topic)) {
                val topicDescription = adminClient.describeTopics(listOf(topic)).values().getValue(topic).get()
                logger.info("Kafka topic $topic already exists: $topicDescription")
                return@useAdminClient
            }
            val newTopic = NewTopic(topic, Optional.of(partitions), Optional.ofNullable(replicationFactor))
            adminClient.createTopics(listOf(newTopic)).all().get()
            logger.info(
                "Created Kafka topic $topic with $partitions partitions" +
                        (replicationFactor?.let { " and replication factor of $it" } ?: "")
            )
        }
    }

    fun <T> useAdminClient(
        brokerReplicaSet: String,
        block: (AdminClient) -> T
    ): T = AdminClient.create(mapOf<String, Any>("bootstrap.servers" to brokerReplicaSet)).use(block)
}
