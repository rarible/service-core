package com.rarible.core.loader

/**
 * API interface to be registered as a `@Component` bean
 * to receive [notifications][LoadNotification].
 */
interface LoadNotificationListener {

    /**
     * The type of tasks this listener wants to listen to.
     */
    val type: LoadType

    /**
     * Callback invoked on receiving a notification.
     */
    suspend fun onLoadNotification(loadNotification: LoadNotification)
}
