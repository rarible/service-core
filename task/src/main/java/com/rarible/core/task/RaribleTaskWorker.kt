package com.rarible.core.task

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.time.delay
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
@FlowPreview
@ExperimentalCoroutinesApi
class RaribleTaskWorker (
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

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationStarted() {
        if (properties.enabled) {
            logger.info("Starting Rarible Task Worker: initDelay={}, poolingDelay={}",
                properties.initialDelay,
                properties.pollingPeriod,
            )
            start()
        } else {
            logger.info("Rarible Task is disabled")
        }
    }

    override suspend fun handle() {
        if (hadInitDelay.get()) {
            delay(pollingPeriod)
        } else {
            delay(properties.initialDelay)
            hadInitDelay.set(true)
            taskService.autorun()
        }
        taskService.runTasks()
    }
}