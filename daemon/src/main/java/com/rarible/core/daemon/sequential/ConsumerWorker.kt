package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.DaemonClosedChannelEvent
import com.rarible.core.daemon.DaemonIncome
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.healthcheck.DowntimeLivenessHealthIndicator
import com.rarible.core.kafka.KafkaConsumer
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
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()
) : SequentialDaemonWorker(meterRegistry, properties, workerName) {

    private val backPressureSize = properties.backpressureSize
    private val downtimeLiveness = DowntimeLivenessHealthIndicator(errorDelay + WORKER_DOWN_TIME)

    @FlowPreview
    @ExperimentalCoroutinesApi
    override suspend fun handle() {
        try {
            consumer.receive()
                .onStart {
                    healthCheck.up()
                }
                .onCompletion {
                    logger.info("Flux was completed")
                }
                .buffer(backPressureSize)
                .collect { event ->
                    onEvent()

                    when (handleInternal(event.value)) {
                        true -> {
                        }
                        false -> {
                            delay(errorDelay)
                            throw AbortFlowException()
                        }
                    }
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

    private suspend fun handleInternal(event: T): Boolean {
        return try {
            eventHandler.handle(event)
            true
        } catch (ex: Exception) {
            logger.error("Can't process event", ex)
            false
        }
    }

    private class AbortFlowException : Exception()

    private companion object {
        val WORKER_DOWN_TIME: Duration = Duration.ofSeconds(30)
    }
}
