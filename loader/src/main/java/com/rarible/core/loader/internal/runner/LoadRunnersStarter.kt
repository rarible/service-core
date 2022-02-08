package com.rarible.core.loader.internal.runner

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.loader.internal.common.KafkaLoadTaskId
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class LoadRunnersStarter(
    private val loadWorkers: ConsumerWorkerHolder<KafkaLoadTaskId>
) {
    @EventListener(ApplicationReadyEvent::class)
    fun startInfrastructure() {
        loadWorkers.start()
    }
}
