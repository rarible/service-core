package com.rarible.core.task

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import java.util.concurrent.atomic.AtomicBoolean

@FlowPreview
@ExperimentalCoroutinesApi
class RaribleTaskWorker(
    taskService: TaskService,
    private val properties: RaribleTaskProperties,
    meterRegistry: MeterRegistry
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(javaClass)

    // We need to incapsulate it to avoid double start if app have other daemons
    private val daemon = TaskWorker(taskService, properties, meterRegistry)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationStarted() {
        if (properties.enabled) {
            logger.info(
                "Starting Rarible Task Worker: initDelay={}, poolingDelay={}, streaming={}",
                properties.initialDelay,
                properties.pollingPeriod,
                properties.streaming,
            )
            daemon.start()
        } else {
            logger.info("Rarible Task is disabled")
        }
    }

    override fun close() {
        daemon.close()
    }
}

@FlowPreview
@ExperimentalCoroutinesApi
private class TaskWorker(
    private val taskService: TaskService,
    private val properties: RaribleTaskProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry,
    DaemonWorkerProperties(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    )
) {

    private val hadInitDelay = AtomicBoolean(false)

    override suspend fun handle() {
        if (!hadInitDelay.get()) {
            delay(properties.initialDelay)
            hadInitDelay.set(true)
            taskService.autorun()
        }
        if (properties.streaming) {
            taskService.runStreamingTaskPoller(properties.pollingPeriod)
        } else {
            taskService.runTaskBatch()
            delay(properties.pollingPeriod)
        }
    }
}
