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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
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
    meterRegistry: MeterRegistry = SimpleMeterRegistry(),
    completionHandler: CompletionHandler? = null
) : SequentialDaemonWorker(meterRegistry, properties, workerName, completionHandler) {

    private val downtimeLiveness = DowntimeLivenessHealthIndicator(errorDelay + WORKER_DOWN_TIME)

    @FlowPreview
    @ExperimentalCoroutinesApi
    override suspend fun handle() {
        try {
            getEventFlow(consumer)
                .onStart { healthCheck.up() }
                .onCompletion { e ->
                    when {
                        e != null && e !is CancellationException -> logger.error("Consumer worker fail $workerName", e)
                        else -> logger.info("Consumer worker has been completed $workerName")
                    }
                }
                .collect { event ->
                    onEventReceived(event)
                    handleInternal(event)
                    onEventHandled(event)
                }
            delay(properties.pollingPeriod)
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
            } catch (e: CancellationException) {
                if (isCancelled) {
                    logger.info("Consumer worker $workerName has been stopped")
                    throw e
                }
                logger.warn("Consumer worker $workerName failed with CancellationException", e)
            } catch (e: Exception) {
                logger.error(
                    buildString {
                        append("Failed to process consumer worker event by $workerName: $event.")
                        if (i < retryProperties.attempts) {
                            append(" Will retry #${i + 1}/${retryProperties.attempts} in ${retryProperties.delay.seconds} s")
                        }
                    }, e
                )
                meterRegistry.increment(DaemonProcessingError(workerName))
                if (i < retryProperties.attempts) {
                    delay(retryProperties.delay)
                }
            }
        }
    }

    private companion object {
        val WORKER_DOWN_TIME: Duration = Duration.ofSeconds(30)
    }

}
