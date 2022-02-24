package com.rarible.core.loader.internal.notification

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.loader.LoadNotification
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class LoadNotificationsStarter(
    private val loadNotificationListenersWorkers: ConsumerWorkerHolder<LoadNotification>
) {
    @EventListener(ApplicationReadyEvent::class)
    fun startInfrastructure() {
        loadNotificationListenersWorkers.start()
    }
}
