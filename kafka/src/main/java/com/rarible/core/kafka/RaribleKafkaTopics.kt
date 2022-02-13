package com.rarible.core.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ListTopicsOptions
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.admin.RecordsToDelete
import org.apache.kafka.common.TopicPartition
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

    fun getAllTopics(brokerReplicaSet: String): List<String> {
        return useAdminClient(brokerReplicaSet) { adminClient ->
            adminClient.listTopics(ListTopicsOptions().listInternal(false))
        }.listings().get().map { it.name() }
    }

    fun removeTopic(
        brokerReplicaSet: String,
        topic: String
    ) {
        useAdminClient(brokerReplicaSet) { adminClient ->
            if (!adminClient.listTopics().names().get().contains(topic)) {
                logger.info("Kafka topic $topic does not exist")
                return@useAdminClient
            }
            logger.info("Removing kafka topic $topic")
            adminClient.deleteTopics(listOf(topic)).all().get()
        }
    }

    fun removeAllRecordsFromTopic(
        brokerReplicaSet: String,
        topic: String
    ) {
        useAdminClient(brokerReplicaSet) { adminClient ->
            val topicDescription = adminClient.describeTopics(listOf(topic)).values()[topic]?.get()
            if (topicDescription == null) {
                logger.info("Kafka topic does not exist: $topic")
                return@useAdminClient
            }
            logger.info("Removing records of Kafka topic $topic")
            val topicPartitions = topicDescription.partitions().map { TopicPartition(topic, it.partition()) }
            val topicPartitionsToDelete = adminClient
                .listOffsets(topicPartitions.associateWith { OffsetSpec.latest() })
                .all()
                .get()
                .mapValues { RecordsToDelete.beforeOffset(it.value.offset()) }
            val topicsDeleteResult = adminClient.deleteRecords(topicPartitionsToDelete)
            for ((topicPartition, resultFuture) in topicsDeleteResult.lowWatermarks()) {
                logger.info(
                    "Removed records from topic's ${topicPartition.topic()} partition #${topicPartition.partition()} " +
                            "up to offset ${resultFuture.get().lowWatermark()}"
                )
            }
        }
    }

    fun <T> useAdminClient(
        brokerReplicaSet: String,
        block: (AdminClient) -> T
    ): T = AdminClient.create(mapOf<String, Any>("bootstrap.servers" to brokerReplicaSet)).use(block)
}
