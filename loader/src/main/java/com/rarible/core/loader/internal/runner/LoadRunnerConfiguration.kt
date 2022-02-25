package com.rarible.core.loader.internal.runner

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.LoadType
import com.rarible.core.loader.Loader
import com.rarible.core.loader.configuration.LOADER_PROPERTIES_PREFIX
import com.rarible.core.loader.configuration.LoadProperties
import com.rarible.core.loader.internal.common.KafkaLoadTaskId
import com.rarible.core.loader.internal.common.LoadCommonConfiguration
import com.rarible.core.loader.internal.common.LoadKafkaTopicsRegistry
import kotlinx.coroutines.CancellationException
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Auto-configuration of the runners part of the loader infrastructure.
 *
 * It defines the runner beans that execute the scheduled tasks.
 */
@ConditionalOnProperty(
    prefix = LOADER_PROPERTIES_PREFIX,
    name = ["enableWorkers"],
    havingValue = "true",
    matchIfMissing = true
)
@Configuration
@Import(LoadCommonConfiguration::class)
@EnableScheduling // Used by [RetryTasksSchedulerSpringJob]
@EnableReactiveMongoRepositories(basePackageClasses = [LoadRunnerPackage::class])
@ComponentScan(basePackageClasses = [LoadRunnerPackage::class])
class LoadRunnerConfiguration {

    private companion object {
        private val logger = LoggerFactory.getLogger(LoadRunnerConfiguration::class.java)

        val CONSUMER_BATCH_MAX_PROCESSING_MS = TimeUnit.MINUTES.toMillis(10).toInt()
    }

    @Bean
    fun loadNotificationKafkaSender(
        loaders: List<Loader>,
        loadProperties: LoadProperties,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): LoadNotificationKafkaSender {
        val kafkaSenders = loaders.map { it.type }.associateWith { type ->
            val uniqueId = UUID.randomUUID().toString()
            createProducerForLoadNotifications(
                bootstrapServers = loadProperties.brokerReplicaSet,
                type = type,
                loadKafkaTopicsRegistry = loadKafkaTopicsRegistry,
                id = uniqueId
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
    fun loadRunnerConfigurationStartupLogger(
        loadProperties: LoadProperties
    ): CommandLineRunner = CommandLineRunner {
        logger.info("Loader workers have been initialized")
    }

    private fun loadWorkers(
        type: LoadType,
        loadProperties: LoadProperties,
        loadRunnerParalleller: LoadRunnerParalleller,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry
    ): List<ConsumerBatchWorker<KafkaLoadTaskId>> {
        val numberOfPartitionsToSubscribeTo = (loadProperties.loadTasksTopicPartitions.toDouble() / loadProperties.loadTasksServiceInstances).roundToInt()
        return (0 until numberOfPartitionsToSubscribeTo).map { id ->
            val uniqueId = UUID.randomUUID().toString()
            val consumer = createConsumerForLoadTasks(
                loadKafkaTopicsRegistry = loadKafkaTopicsRegistry,
                bootstrapServers = loadProperties.brokerReplicaSet,
                type = type,
                id = uniqueId
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
                properties = DaemonWorkerProperties(consumerBatchSize = loadProperties.loadTasksBatchSize),
                retryProperties = RetryProperties(attempts = 1, delay = Duration.ZERO),
                completionHandler = {
                    if (it != null && it !is CancellationException) {
                        logger.error("Loading worker $workerName aborted because of a fatal error", it)
                    }
                }
            )
        }
    }

    private fun createConsumerForLoadTasks(
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry,
        bootstrapServers: String,
        type: LoadType,
        id: String,
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

    private fun createProducerForLoadNotifications(
        bootstrapServers: String,
        type: LoadType,
        loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry,
        id: String
    ): RaribleKafkaProducer<LoadNotification> = RaribleKafkaProducer(
        clientId = "loader-notification-consumer-$type-$id",
        valueSerializerClass = JsonSerializer::class.java,
        defaultTopic = loadKafkaTopicsRegistry.getLoadNotificationsTopic(type),
        bootstrapServers = bootstrapServers,
        valueClass = LoadNotification::class.java
    )

}
