package com.rarible.core.loader.internal

import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.LoadNotificationListener
import com.rarible.core.loader.LoadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LoadNotificationListenersCaller(
    loadNotificationListeners: List<LoadNotificationListener>
) {

    private val logger = LoggerFactory.getLogger(LoadNotificationListenersCaller::class.java)

    private val loadNotificationListeners: Map<LoadType, List<LoadNotificationListener>> =
        loadNotificationListeners.groupBy { it.type }

    suspend fun notifyListeners(loadNotification: LoadNotification) {
        val listeners = loadNotificationListeners[loadNotification.type]
        if (listeners == null || listeners.isEmpty()) {
            logger.warn("No notification listeners are registered for type ${loadNotification.type}")
            return
        }
        for (listener in listeners) {
            try {
                listener.onLoadNotification(loadNotification)
            } catch (e: Exception) {
                logger.error("Failed to call notification listener for $loadNotification", e)
            }
        }
    }
}
