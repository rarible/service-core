package com.rarible.core.loader.internal.common

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.loader.LoadType
import com.rarible.core.loader.Loader
import com.rarible.core.loader.configuration.LoadProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.time.Clock

/**
 * Configuration of the scheduling part of the loader infrastructure.
 * It defines the beans used to send scheduled tasks to Kafka
 * and notify listeners when the tasks' statuses change.
 */
@Configuration
@ComponentScan(basePackageClasses = [LoadCommonPackage::class])
@EnableReactiveMongoRepositories(basePackageClasses = [LoadCommonPackage::class])
@EnableConfigurationProperties(LoadProperties::class)
class LoadCommonConfiguration {

    private val logger = LoggerFactory.getLogger(LoadCommonConfiguration::class.java)

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
    fun loadCommonConfigurationStartupLogger(
        loadProperties: LoadProperties
    ): CommandLineRunner = CommandLineRunner {
        logger.info("Loader infrastructure has been initialized with properties $loadProperties")
    }

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
}
