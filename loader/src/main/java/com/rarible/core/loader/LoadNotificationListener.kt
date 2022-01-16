package com.rarible.core.loader

/**
 * API interface to be registered as a `@Component` bean
 * to receive [notifications][LoadNotification] about completed and failed tasks.
 */
interface LoadNotificationListener {

    /**
     * The type of tasks this listener is wishing to listen to.
     */
    val type: LoadType

    /**
     * Callback invoked on receiving a notification about completion or failure of a task.
     */
    suspend fun onLoadNotification(loadNotification: LoadNotification)
}
