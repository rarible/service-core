package com.rarible.core.loader.internal

import com.rarible.core.kafka.RaribleKafkaTopics
import com.rarible.core.loader.LoadType
import com.rarible.core.loader.configuration.LoadProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LoadKafkaTopicsRegistry(
    private val loadProperties: LoadProperties
) {
    private val logger = LoggerFactory.getLogger(LoadKafkaTopicsRegistry::class.java)

    fun getLoadTasksTopic(type: LoadType) =
        "${loadProperties.topicsPrefix}.loader-tasks-$type"

    fun getLoadNotificationsTopic(type: LoadType) =
        "${loadProperties.topicsPrefix}.loader-notifications-$type"

    fun createTopics(loadTypes: List<LoadType>) {
        for (type in loadTypes) {
            val loadTasksTopic = getLoadTasksTopic(type)
            logger.info("Creating topic (if necessary) $loadTasksTopic with ${loadProperties.loadTasksTopicPartitions} partitions")
            RaribleKafkaTopics.createTopic(
                brokerReplicaSet = loadProperties.brokerReplicaSet,
                topic = loadTasksTopic,
                partitions = loadProperties.loadTasksTopicPartitions
            )

            val notificationsTopic = getLoadNotificationsTopic(type)
            logger.info("Creating topic (if necessary) $notificationsTopic with ${loadProperties.loadNotificationsTopicPartitions} partitions")
            RaribleKafkaTopics.createTopic(
                brokerReplicaSet = loadProperties.brokerReplicaSet,
                topic = notificationsTopic,
                partitions = loadProperties.loadNotificationsTopicPartitions
            )
        }
    }
}
