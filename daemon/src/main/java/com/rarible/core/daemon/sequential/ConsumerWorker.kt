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
import kotlinx.coroutines.time.delay
import java.nio.channels.ClosedChannelException
import java.time.Duration

class ConsumerWorker<T>(
    private val consumer: KafkaConsumer<T>,
    private val eventHandler: ConsumerEventHandler<T>,
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
            consumer.receiveManualAcknowledge()
                .onStart {
                    healthCheck.up()
                }
                .onCompletion {
                    logger.info("Flux was completed")
                }.let {
                    if (properties.buffer) {
                        it.buffer(backPressureSize)
                    } else {
                        it
                    }
                }
                .collect { event ->
                    onEvent()

                    handleInternal(event)

                    event.receiverRecord?.receiverOffset()?.acknowledge()
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

    private fun onEvent() {
        meterRegistry.increment(DaemonIncome(workerName))
    }

    private suspend fun handleInternal(event: KafkaMessage<T>) {
        for (i in (1..retryProperties.attempts)) {
            try {
                eventHandler.handle(event.value)
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
