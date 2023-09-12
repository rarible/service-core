package com.rarible.core.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.BatchMessageListener
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

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
        settings: RaribleKafkaConsumerSettings,
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
        settings: RaribleKafkaConsumerSettings,
        handler: RaribleKafkaBatchEventHandler<T>,
        factory: RaribleKafkaListenerContainerFactory<T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        val listener = RaribleKafkaMessageListenerFactory.create(handler, settings.async)
        return createWorker(settings, listener, factory)
    }

    private fun <T> createWorker(
        settings: RaribleKafkaConsumerSettings,
        listener: SuspendBatchMessageListener<String, T>,
        factory: RaribleKafkaListenerContainerFactory<T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        logger.info("Created Kafka consumer with params: {}", settings)
        val container = factory.createContainer(settings.topic)

        val wrappedListener = when {
            settings.coroutineThreadCount <= 1 -> BlockingListener(listener)
            else -> CoroutineListener(listener, settings.coroutineThreadCount)
        }

        container.setupMessageListener(wrappedListener)
        container.containerProperties.groupId = "$env.${settings.group}"
        container.containerProperties.clientId = "$clientIdPrefix.${settings.group}"

        return container
    }

    private interface CloseableBatchMessageListener<K, T> : BatchMessageListener<K, T>, AutoCloseable

    private class BlockingListener<T>(
        private val listener: SuspendBatchMessageListener<String, T>
    ) : CloseableBatchMessageListener<String, T> {

        override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking {
            listener.onMessage(records)
        }

        override fun close() {
            // Nothing to do
        }
    }

    private class CoroutineListener<T>(
        private val listener: SuspendBatchMessageListener<String, T>,
        coroutineThreads: Int,
    ) : CloseableBatchMessageListener<String, T> {

        private val threadPrefix = "${THREAD_PREFIX.incrementAndGet()}-KafkaSuspendListener"

        private val daemonDispatcher = Executors.newFixedThreadPool(coroutineThreads) {
            Thread(it, "$threadPrefix-${THREAD_INDEX.getAndIncrement()}")
        }.asCoroutineDispatcher()

        private val scope = CoroutineScope(SupervisorJob() + daemonDispatcher)

        override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking {
            val job = scope.launch { listener.onMessage(records) }
            job.join()
        }

        override fun close() {
            scope.cancel()
            daemonDispatcher.close()
        }

        private companion object {
            val THREAD_INDEX = AtomicInteger()
            val THREAD_PREFIX = AtomicInteger()
        }
    }
}
