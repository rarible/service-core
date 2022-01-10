package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.DaemonIncome
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.kafka.KafkaConsumer
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.telemetry.metrics.increment
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.Flow

class ConsumerWorker<T>(
    consumer: KafkaConsumer<T>,
    private val eventHandler: ConsumerEventHandler<T>,
    workerName: String,
    properties: DaemonWorkerProperties = DaemonWorkerProperties(),
    retryProperties: RetryProperties = RetryProperties(),
    meterRegistry: MeterRegistry = SimpleMeterRegistry(),
    completionHandler: CompletionHandler? = null
) : AbstractConsumerWorker<T, KafkaMessage<T>>(
    consumer = consumer,
    workerName = workerName,
    properties = properties,
    retryProperties = retryProperties,
    meterRegistry = meterRegistry,
    completionHandler = completionHandler
) {

    override fun getEventFlow(consumer: KafkaConsumer<T>): Flow<KafkaMessage<T>> {
        return consumer.receiveManualAcknowledge()
    }

    override suspend fun onEventReceived(event: KafkaMessage<T>) {
        meterRegistry.increment(DaemonIncome(workerName))
    }

    override suspend fun handle(event: KafkaMessage<T>) {
        eventHandler.handle(event.value)
    }

    override suspend fun onEventHandled(event: KafkaMessage<T>) {
        event.receiverRecord?.receiverOffset()?.acknowledge()
    }
}
