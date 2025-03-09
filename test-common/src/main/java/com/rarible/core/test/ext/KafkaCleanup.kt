package com.rarible.core.test.ext

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ConsumerGroupListing
import org.apache.kafka.clients.admin.ListTopicsResult
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicCollection
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.annotation.Inherited
import java.util.stream.Collectors

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(KafkaCleanupExtension::class)
annotation class KafkaCleanup

class KafkaCleanupExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext?) {
        AdminClient.create(mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaTestExtension.kafkaContainer.kafkaBoostrapServers())).use { adminClient ->

            // Delete all topics
            val result: ListTopicsResult = adminClient.listTopics()
            val topicNames = result.names().get()
            topicNames.remove("_schemas")
            adminClient.deleteTopics(TopicCollection.ofTopicNames(topicNames)).all().get()

            // Delete all consumer groups
            val consumerGroups: List<String> = adminClient
                .listConsumerGroups().all().get()
                .stream()
                .map { obj: ConsumerGroupListing -> obj.groupId() }
                .collect(Collectors.toList())
            adminClient.deleteConsumerGroups(consumerGroups).all().get()
        }
    }
}
