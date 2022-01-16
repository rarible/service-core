package com.rarible.core.loader.internal

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaTopics
import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.LoadType
import com.rarible.core.loader.Loader
import com.rarible.core.loader.configuration.LoadProperties
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.stereotype.Component

@Component
class LoadInfrastructureInitializer(
    private val loadProperties: LoadProperties,
    private val mongo: ReactiveMongoOperations,

    private val loaders: List<Loader>,
    private val loadWorkers: ConsumerWorkerHolder<KafkaLoadTaskId>,
    private val loadNotificationListenersWorkers: ConsumerWorkerHolder<LoadNotification>
) {

    private val infraLogger = LoggerFactory.getLogger(LoadInfrastructureInitializer::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun startInfrastructure() {
        val loadTypes = loaders.map { it.type }
        createTopics(loadTypes)
        createMongoIndexes()
        loadWorkers.start()
        loadNotificationListenersWorkers.start()
    }

    private fun createMongoIndexes() {
        runBlocking {
            LoadTaskRepositoryIndexes.ensureIndexes(mongo)
        }
    }

    private fun createTopics(loadTypes: List<LoadType>) {
        for (type in loadTypes) {
            val loadTasksTopic = getLoadTasksTopic(type)
            infraLogger.info("Creating topic (if necessary) $loadTasksTopic with ${loadProperties.loadTasksTopicPartitions} partitions")
            RaribleKafkaTopics.createTopic(
                brokerReplicaSet = loadProperties.brokerReplicaSet,
                topic = loadTasksTopic,
                partitions = loadProperties.loadTasksTopicPartitions
            )

            val notificationsTopic = getLoadNotificationsTopic(type)
            infraLogger.info("Creating topic (if necessary) $notificationsTopic with ${loadProperties.loadNotificationsTopicPartitions} partitions")
            RaribleKafkaTopics.createTopic(
                brokerReplicaSet = loadProperties.brokerReplicaSet,
                topic = notificationsTopic,
                partitions = loadProperties.loadNotificationsTopicPartitions
            )
        }
    }
}
