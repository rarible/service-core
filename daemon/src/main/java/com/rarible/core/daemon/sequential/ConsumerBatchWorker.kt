package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.DaemonIncome
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.kafka.KafkaConsumer
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.telemetry.metrics.increment
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class ConsumerBatchWorker<T>(
    consumer: KafkaConsumer<T>,
    private val eventHandler: ConsumerBatchEventHandler<T>,
    workerName: String,
    properties: DaemonWorkerProperties = DaemonWorkerProperties(),
    retryProperties: RetryProperties = RetryProperties(),
    meterRegistry: MeterRegistry = SimpleMeterRegistry()
) : AbstractConsumerWorker<T, List<KafkaMessage<T>>>(
    consumer, workerName, properties, retryProperties, meterRegistry
) {

    override fun getEventFlow(consumer: KafkaConsumer<T>): Flow<List<KafkaMessage<T>>> {
        return consumer.receiveBatch().map { it.toList() }
    }

    override suspend fun onEventReceived(event: List<KafkaMessage<T>>) {
        meterRegistry.increment(DaemonIncome(workerName), event.size)
    }

    override suspend fun handle(event: List<KafkaMessage<T>>) {
        eventHandler.handle(event.map { it.value })
    }

    override suspend fun onEventHandled(event: List<KafkaMessage<T>>) {
        // Do nothing, aut ack enabled here
    }
}