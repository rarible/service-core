package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.DaemonClosedChannelEvent
import com.rarible.core.daemon.DaemonProcessingError
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.healthcheck.DowntimeLivenessHealthIndicator
import com.rarible.core.kafka.KafkaConsumer
import com.rarible.core.telemetry.metrics.increment
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.time.delay
import java.nio.channels.ClosedChannelException
import java.time.Duration

abstract class AbstractConsumerWorker<T, E>(
    private val consumer: KafkaConsumer<T>,
    workerName: String,
    private val properties: DaemonWorkerProperties = DaemonWorkerProperties(),
    private val retryProperties: RetryProperties = RetryProperties(),
    protected val meterRegistry: MeterRegistry = SimpleMeterRegistry()
) : SequentialDaemonWorker(meterRegistry, properties, workerName) {

    protected val backPressureSize = properties.backpressureSize
    protected val downtimeLiveness = DowntimeLivenessHealthIndicator(errorDelay + WORKER_DOWN_TIME)

    @FlowPreview
    @ExperimentalCoroutinesApi
    override suspend fun handle() {
        try {
            getEventFlow(consumer)
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
                    onEventReceived(event)
                    handleInternal(event)
                    onEventHandled(event)
                }
            delay(properties.pollingPeriod)
        } catch (ignored: AbortFlowException) {
        } catch (ignored: ClosedChannelException) {
            logger.warn("Channel was closed", ignored)
            meterRegistry.increment(DaemonClosedChannelEvent(workerName))
            delay(WORKER_DOWN_TIME)
        }
    }

    abstract fun getEventFlow(consumer: KafkaConsumer<T>): Flow<E>

    abstract suspend fun onEventReceived(event: E)

    abstract suspend fun handle(event: E)

    abstract suspend fun onEventHandled(event: E)

    override fun health() = downtimeLiveness.health()

    private suspend fun handleInternal(event: E) {
        for (i in (1..retryProperties.attempts)) {
            try {
                handle(event)
                return
            } catch (ex: Exception) {
                logger.error("Can't process event $event", ex)
                meterRegistry.increment(DaemonProcessingError(workerName))
                if (i < retryProperties.attempts) {
                    logger.info("Retrying in ${retryProperties.delay}. Attempt #${i + 1}")
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