package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.*
import com.rarible.core.daemon.healthcheck.DowntimeLivenessHealthIndicator
import com.rarible.core.kafka.KafkaConsumer
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.telemetry.metrics.increment
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.time.delay
import java.nio.channels.ClosedChannelException
import java.time.Duration

class BatchConsumerWorker<T>(
    private val consumer: KafkaConsumer<T>,
    private val eventHandler: BatchConsumerEventHandler<T>,
    workerName: String,
    private val properties: DaemonWorkerProperties = DaemonWorkerProperties(),
    private val retryProperties: RetryProperties = RetryProperties(),
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()
) : SequentialDaemonWorker(meterRegistry, properties, workerName) {

    private val backPressureSize = properties.backpressureSize
    private val downtimeLiveness = DowntimeLivenessHealthIndicator(errorDelay + WORKER_DOWN_TIME)

    @FlowPreview
    @ExperimentalCoroutinesApi
    override suspend fun handle() {
        try {
            consumer.receiveBatchManualAcknowledge(properties.batchSize, properties.batchWindowTimeout)
                .onStart {
                    healthCheck.up()
                }
                .onCompletion {
                    logger.info("Flux was completed")
                }
                .collect { events ->
                    val eventsList = events.toList()
                    onEvent(eventsList.size)

                    handleInternal(eventsList)

                    eventsList.forEach { it.receiverRecord?.receiverOffset()?.acknowledge() }
                }
            delay(properties.pollingPeriod)
        } catch (ignored: AbortFlowException) {
        } catch (ignored: ClosedChannelException) {
            logger.warn("Channel was closed", ignored)
            meterRegistry.increment(DaemonClosedChannelEvent(workerName))
            delay(WORKER_DOWN_TIME)
        }
    }

    override fun health() = downtimeLiveness.health()

    private fun onEvent(size: Int) {
        meterRegistry.increment(DaemonIncome(workerName), size)
    }

    private suspend fun handleInternal(event: List<KafkaMessage<T>>) {
        for (i in (1..retryProperties.attempts)) {
            try {
                eventHandler.handle(event.map { it.value })
                return
            } catch (ex: Exception) {
                logger.error("Can't process event $event", ex)
                meterRegistry.increment(DaemonProcessingError(workerName))
                if (i < retryProperties.attempts) {
                    logger.info("Retrying in ${retryProperties.delay}. Attempt #${i+1}")
                    delay(retryProperties.delay)
                }
            }
        }
    }

    private class AbortFlowException : Exception()

    private companion object {
        val WORKER_DOWN_TIME: Duration = Duration.ofSeconds(30)
    }
}
