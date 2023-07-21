package com.rarible.protocol.apikey.kafka

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.core.common.asyncWithTraceId
import com.rarible.core.logging.withBatchId
import com.rarible.core.logging.withTraceId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.BatchMessageListener

object RaribleKafkaMessageListenerFactory {

    fun <T> create(handler: RaribleKafkaEventHandler<T>, async: Boolean): BatchMessageListener<String, T> {
        return when (async) {
            // All events will be handled in parallel (except that having same key)
            true -> Concurrent(handler)
            // All events will be handled one-by-one
            false -> Sequential(handler)
        }
    }

    fun <T> create(handler: RaribleKafkaBatchEventHandler<T>, async: Boolean): BatchMessageListener<String, T> {
        return when (async) {
            // All events grouped by key will be handled in parallel, all events in the batch have same key
            true -> BatchConcurrent(handler)
            // Batch will be handled entirely by handler, concurrency implementation lies on handler's implementation
            false -> BatchSequential(handler)
        }
    }

    private class Concurrent<T>(
        private val handler: RaribleKafkaEventHandler<T>
    ) : BatchMessageListener<String, T> {
        override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking<Unit>(NonCancellable) {
            withBatchId {
                val recordsByKey = records.filter { it.value() != null }.groupBy { it.key() }
                recordsByKey.values.map { group ->
                    asyncWithTraceId(context = NonCancellable) {
                        withTraceId {
                            group.forEach {
                                handler.handle(it.value())
                            }
                        }
                    }
                }.awaitAll()
            }
        }
    }

    private class Sequential<T>(
        private val handler: RaribleKafkaEventHandler<T>
    ) : BatchMessageListener<String, T> {

        override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking<Unit>(NonCancellable) {
            withBatchId {
                records.map { record ->
                    record.value()?.let {
                        withTraceId {
                            handler.handle(it)
                        }
                    }
                }
            }
        }
    }

    private class BatchSequential<T>(
        private val handler: RaribleKafkaBatchEventHandler<T>
    ) : BatchMessageListener<String, T> {

        override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking<Unit>(NonCancellable) {
            withBatchId {
                withTraceId {
                    handler.handle(records.mapNotNull { it.value() })
                }
            }
        }
    }

    private class BatchConcurrent<T>(
        private val handler: RaribleKafkaBatchEventHandler<T>
    ) : BatchMessageListener<String, T> {

        override fun onMessage(records: List<ConsumerRecord<String, T>>) = runBlocking<Unit>(NonCancellable) {
            withBatchId {
                val recordsByKey = records.filter { it.value() != null }.groupBy { it.key() }
                recordsByKey.values.map { group ->
                    asyncWithTraceId(context = NonCancellable) {
                        withTraceId {
                            handler.handle(group.map { it.value() })
                        }
                    }
                }.awaitAll()
            }
        }
    }
}
