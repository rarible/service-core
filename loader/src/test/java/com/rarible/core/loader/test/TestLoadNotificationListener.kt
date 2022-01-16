package com.rarible.core.loader.test

import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.LoadNotificationListener
import com.rarible.core.loader.LoadType
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

val testLoaderType = "test-load-${System.currentTimeMillis()}"

val testReceivedNotifications: CopyOnWriteArrayList<LoadNotification> = CopyOnWriteArrayList()

@Component
class TestLoadNotificationListener : LoadNotificationListener {
    override val type: LoadType = testLoaderType

    override suspend fun onLoadNotification(loadNotification: LoadNotification) {
        println("Received load notification $loadNotification")
        testReceivedNotifications += loadNotification
    }
}
