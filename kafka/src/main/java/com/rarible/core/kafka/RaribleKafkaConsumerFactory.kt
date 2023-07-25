package com.rarible.core.kafka

import com.rarible.protocol.apikey.kafka.RaribleKafkaMessageListenerFactory
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.kafka.listener.AbstractMessageListenerContainer
import org.springframework.kafka.listener.BatchMessageListener
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import java.util.UUID

class RaribleKafkaConsumerFactory(
    private val env: String,
    private val host: String,
    private val deserializer: Class<*>? = null
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
        handler: RaribleKafkaEventHandler<T>
    ): RaribleKafkaConsumerWorker<T> {
        val listener = RaribleKafkaMessageListenerFactory.create(handler, settings.async)
        return createWorker(settings, listener)
    }

    /**
     * Creates worker for batch handling. Can group messages by kafka 'key' to smaller batches
     * and call handle() function in async way, if settings.async == true
     */
    fun <T> createWorker(
        settings: RaribleKafkaConsumerSettings<T>,
        handler: RaribleKafkaBatchEventHandler<T>
    ): RaribleKafkaConsumerWorker<T> {
        val listener = RaribleKafkaMessageListenerFactory.create(handler, settings.async)
        return createWorker(settings, listener)
    }

    private fun <T> createWorker(
        settings: RaribleKafkaConsumerSettings<T>,
        listener: BatchMessageListener<String, T>
    ): RaribleKafkaConsumerWorker<T> {
        val factory = RaribleKafkaListenerContainerFactory(
            valueClass = settings.valueClass,
            concurrency = settings.concurrency,
            hosts = settings.hosts,
            batchSize = settings.batchSize,
            offsetResetStrategy = settings.offsetResetStrategy,
            customSettings = customConfig()
        )

        logger.info("Created Kafka consumer with params: {}", settings)
        val container = factory.createContainer(settings.topic)
        container.setupMessageListener(listener)
        container.containerProperties.groupId = "${env}.${settings.group}"
        container.containerProperties.clientId = "$clientIdPrefix.${settings.group}"

        return RaribleKafkaConsumerWorkerWrapper(listOf(container))
    }

    private fun customConfig(): Map<String, Any> {
        val config = HashMap<String, Any>()
        deserializer?.let { config[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = it }
        return config
    }

    class RaribleKafkaConsumerWorkerWrapper<K, V>(
        private val containers: List<AbstractMessageListenerContainer<K, V>>
    ) : RaribleKafkaConsumerWorker<V>, ApplicationEventPublisherAware, ApplicationContextAware {

        override fun start() {
            containers.forEach(AbstractMessageListenerContainer<K, V>::start)
        }

        override fun close() {
            containers.forEach(AbstractMessageListenerContainer<K, V>::start)
        }

        override fun setApplicationEventPublisher(applicationEventPublisher: ApplicationEventPublisher) {
            containers.forEach { it.setApplicationEventPublisher(applicationEventPublisher) }
        }

        override fun setApplicationContext(applicationContext: ApplicationContext) {
            containers.forEach { it.setApplicationContext(applicationContext) }
        }
    }
}