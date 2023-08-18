package com.rarible.core.kafka

import com.rarible.protocol.apikey.kafka.RaribleKafkaMessageListenerFactory
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.BatchMessageListener
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.util.UUID

class RaribleKafkaConsumerFactory(
    private val env: String,
    host: String,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RaribleKafkaConsumerFactory::class.java)
    }

    private val clientIdPrefix = "$env.$host.${UUID.randomUUID()}"

    /**
     * Creates worker for one-by-one message handler. Can group messages by kafka 'key'
     * and call handle() function in async way, if settings.async == true
     */
    fun <T> createWorker(
        settings: RaribleKafkaConsumerSettings<T>,
        handler: RaribleKafkaEventHandler<T>,
        factory: RaribleKafkaListenerContainerFactory<T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        val listener = RaribleKafkaMessageListenerFactory.create(handler, settings.async)
        return createWorker(settings, listener, factory)
    }

    /**
     * Creates worker for batch handling. Can group messages by kafka 'key' to smaller batches
     * and call handle() function in async way, if settings.async == true
     */
    fun <T> createWorker(
        settings: RaribleKafkaConsumerSettings<T>,
        handler: RaribleKafkaBatchEventHandler<T>,
        factory: RaribleKafkaListenerContainerFactory<T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        val listener = RaribleKafkaMessageListenerFactory.create(handler, settings.async)
        return createWorker(settings, listener, factory)
    }

    private fun <T> createWorker(
        settings: RaribleKafkaConsumerSettings<T>,
        listener: BatchMessageListener<String, T>,
        factory: RaribleKafkaListenerContainerFactory<T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        logger.info("Created Kafka consumer with params: {}", settings)
        val container = factory.createContainer(settings.topic)
        container.setupMessageListener(listener)
        container.containerProperties.groupId = "${env}.${settings.group}"
        container.containerProperties.clientId = "$clientIdPrefix.${settings.group}"

        return container
    }
}