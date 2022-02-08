package com.rarible.core.loader.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.LoadService
import com.rarible.core.loader.LoadType
import com.rarible.core.loader.Loader
import com.rarible.core.loader.internal.KafkaLoadTaskId
import com.rarible.core.loader.internal.LoadKafkaTopicsRegistry
import com.rarible.core.loader.internal.LoadNotificationKafkaSender
import com.rarible.core.loader.internal.LoadNotificationListenersCaller
import com.rarible.core.loader.internal.LoadRunner
import com.rarible.core.loader.internal.LoadRunnerParalleller
import com.rarible.core.loader.internal.LoadTaskKafkaSender
import kotlinx.coroutines.CancellationException
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Auto-configuration of the loader infrastructure.
 *
 * It defines the main worker beans used for loading tasks and notifying listeners.
 */
@Configuration
@EnableScheduling
@EnableReactiveMongoRepositories(basePackageClasses = [LoadService::class])
@ComponentScan(basePackageClasses = [LoadService::class])
@EnableConfigurationProperties(LoadProperties::class)
class LoaderConfiguration {

    private companion object {
        private val logger = LoggerFactory.getLogger(LoaderConfiguration::class.java)

        const val CONSUMER_BATCH_SIZE = 500

        val CONSUMER_BATCH_MAX_PROCESSING_MS = TimeUnit.MINUTES.toMillis(10).toInt()
    }

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun loadTaskKafkaSender(
        loaders: List<Loader>,
        loadProperties: LoadProperties,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): LoadTaskKafkaSender {
        val kafkaSenders = loaders.map { it.type }.associateWith { type ->
            createProducerForLoadTasks(
                loadKafkaTopicsRegistry = loadKafkaTopicsRegistry,
                bootstrapServers = loadProperties.brokerReplicaSet,
                type = type
            )
        }
        return LoadTaskKafkaSender(kafkaSenders)
    }

    @Bean
    fun loadNotificationKafkaSender(
        loaders: List<Loader>,
        loadProperties: LoadProperties,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): LoadNotificationKafkaSender {
        val kafkaSenders = loaders.map { it.type }.associateWith { type ->
            createProducerForLoadNotifications(
                bootstrapServers = loadProperties.brokerReplicaSet,
                type = type,
                loadKafkaTopicsRegistry = loadKafkaTopicsRegistry
            )
        }
        return LoadNotificationKafkaSender(kafkaSenders)
    }

    @Bean
    fun loadWorkers(
        loaders: List<Loader>,
        loadProperties: LoadProperties,
        loadRunner: LoadRunner,
        loadRunnerParalleller: LoadRunnerParalleller,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): ConsumerWorkerHolder<KafkaLoadTaskId> = ConsumerWorkerHolder(
        loaders.flatMap { loader ->
            loadWorkers(
                type = loader.type,
                loadProperties = loadProperties,
                loadRunnerParalleller = loadRunnerParalleller,
                loadKafkaTopicsRegistry = loadKafkaTopicsRegistry
            )
        }
    )

    @Bean
    fun loadRunnerParalleller(
        loadRunner: LoadRunner,
        loadProperties: LoadProperties
    ) = LoadRunnerParalleller(
        numberOfWorkingThreads = loadProperties.workers,
        loadRunner = loadRunner
    )

    @Bean
    fun notificationListenersWorkers(
        loaders: List<Loader>,
        loadProperties: LoadProperties,
        loadNotificationListenersCaller: LoadNotificationListenersCaller,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): ConsumerWorkerHolder<LoadNotification> = ConsumerWorkerHolder(
        loaders.flatMap { loader ->
            notificationListenersWorkers(
                type = loader.type,
                loadProperties = loadProperties,
                loadNotificationListenersCaller = loadNotificationListenersCaller,
                loadKafkaTopicsRegistry = loadKafkaTopicsRegistry
            )
        }
    )

    private fun loadWorkers(
        type: LoadType,
        loadProperties: LoadProperties,
        loadRunnerParalleller: LoadRunnerParalleller,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): List<ConsumerBatchWorker<KafkaLoadTaskId>> {
        return (0 until loadProperties.loadTasksTopicPartitions).map { id ->
            val consumer = createConsumerForLoadTasks(
                loadKafkaTopicsRegistry = loadKafkaTopicsRegistry,
                bootstrapServers = loadProperties.brokerReplicaSet,
                type = type,
                id = id
            )
            val workerName = "load-worker-$type-$id"
            logger.info("Creating load worker $workerName")
            ConsumerBatchWorker(
                consumer = consumer,
                eventHandler = object : ConsumerBatchEventHandler<KafkaLoadTaskId> {
                    override suspend fun handle(event: List<KafkaLoadTaskId>) {
                        loadRunnerParalleller.load(event)
                    }
                },
                workerName = workerName,
                properties = DaemonWorkerProperties(consumerBatchSize = CONSUMER_BATCH_SIZE),
                retryProperties = RetryProperties(attempts = 1, delay = Duration.ZERO),
                completionHandler = {
                    if (it != null && it !is CancellationException) {
                        logger.error("Loading worker $workerName aborted because of a fatal error", it)
                    }
                }
            )
        }
    }

    private fun notificationListenersWorkers(
        type: LoadType,
        loadProperties: LoadProperties,
        loadNotificationListenersCaller: LoadNotificationListenersCaller,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): List<ConsumerWorker<LoadNotification>> {
        return (0 until loadProperties.loadNotificationsTopicPartitions).map { id ->
            val consumer = createConsumerForLoadNotifications(
                bootstrapServers = loadProperties.brokerReplicaSet,
                type = type,
                id = id,
                loadKafkaTopicsRegistry = loadKafkaTopicsRegistry
            )
            val notificationsLogger = LoggerFactory.getLogger("loading-notifications-$type")
            val workerName = "loader-notification-consumer-$type-$id"
            logger.info("Creating notification listener worker $workerName")
            ConsumerWorker(
                consumer = consumer,
                eventHandler = object : ConsumerEventHandler<LoadNotification> {
                    override suspend fun handle(event: LoadNotification) {
                        notificationsLogger.info("Received load notification $event")
                        loadNotificationListenersCaller.notifyListeners(event)
                    }
                },
                workerName = workerName,
                retryProperties = RetryProperties(attempts = 1, delay = Duration.ZERO),
                completionHandler = {
                    if (it != null && it !is CancellationException) {
                        logger.error("Notifications listener $workerName aborted because of a fatal error", it)
                    }
                }
            )
        }
    }

    private fun createConsumerForLoadTasks(
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry,
        bootstrapServers: String,
        type: LoadType,
        id: Int,
    ): RaribleKafkaConsumer<KafkaLoadTaskId> = RaribleKafkaConsumer(
        clientId = "loader-tasks-consumer-$type-$id",
        consumerGroup = "loader-tasks-group-$type",
        valueDeserializerClass = JsonDeserializer::class.java,
        defaultTopic = loadKafkaTopicsRegistry.getLoadTasksTopic(type),
        bootstrapServers = bootstrapServers,
        offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        valueClass = KafkaLoadTaskId::class.java,
        autoCreateTopic = false,
        maxPollIntervalMs = CONSUMER_BATCH_MAX_PROCESSING_MS
    )

    private fun createConsumerForLoadNotifications(
        bootstrapServers: String,
        type: LoadType,
        id: Int,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): RaribleKafkaConsumer<LoadNotification> = RaribleKafkaConsumer(
        clientId = "loader-notifications-consumer-$type-$id",
        consumerGroup = "loader-notifications-$type",
        valueDeserializerClass = JsonDeserializer::class.java,
        defaultTopic = loadKafkaTopicsRegistry.getLoadNotificationsTopic(type),
        bootstrapServers = bootstrapServers,
        offsetResetStrategy = OffsetResetStrategy.EARLIEST,
        valueClass = LoadNotification::class.java,
        autoCreateTopic = false
    )

    private fun createProducerForLoadTasks(
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry,
        bootstrapServers: String,
        type: LoadType
    ): RaribleKafkaProducer<KafkaLoadTaskId> = RaribleKafkaProducer(
        clientId = "loader-task-client-$type",
        valueSerializerClass = JsonSerializer::class.java,
        defaultTopic = loadKafkaTopicsRegistry.getLoadTasksTopic(type),
        bootstrapServers = bootstrapServers,
        valueClass = KafkaLoadTaskId::class.java
    )

    private fun createProducerForLoadNotifications(
        bootstrapServers: String,
        type: LoadType,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): RaribleKafkaProducer<LoadNotification> = RaribleKafkaProducer(
        clientId = "loader-notification-client-$type",
        valueSerializerClass = JsonSerializer::class.java,
        defaultTopic = loadKafkaTopicsRegistry.getLoadNotificationsTopic(type),
        bootstrapServers = bootstrapServers,
        valueClass = LoadNotification::class.java
    )

}
