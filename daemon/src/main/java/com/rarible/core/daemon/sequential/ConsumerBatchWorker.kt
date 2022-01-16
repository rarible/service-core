package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.DaemonIncome
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.kafka.KafkaConsumer
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaMessageBatch
import com.rarible.core.telemetry.metrics.increment
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConsumerBatchWorker<T>(
    consumer: KafkaConsumer<T>,
    private val eventHandler: ConsumerBatchEventHandler<T>,
    workerName: String,
    private val properties: DaemonWorkerProperties = DaemonWorkerProperties(),
    retryProperties: RetryProperties = RetryProperties(),
    meterRegistry: MeterRegistry = SimpleMeterRegistry(),
    completionHandler: CompletionHandler? = null
) : AbstractConsumerWorker<T, KafkaMessageBatch<T>>(
    consumer = consumer,
    workerName = workerName,
    properties = properties,
    retryProperties = retryProperties,
    meterRegistry = meterRegistry,
    completionHandler = completionHandler
) {

    override fun getEventFlow(consumer: KafkaConsumer<T>): Flow<KafkaMessageBatch<T>> =
        consumer.receiveBatchManualAck(properties.consumerBatchSize)

    override suspend fun onEventReceived(event: KafkaMessageBatch<T>) {
        meterRegistry.increment(DaemonIncome(workerName), event.messages.size)
    }

    override suspend fun handle(event: KafkaMessageBatch<T>) {
        eventHandler.handle(event.messages.map { it.value })
    }

    override suspend fun onEventHandled(event: KafkaMessageBatch<T>) {
        event.acknowledge()
    }
}
