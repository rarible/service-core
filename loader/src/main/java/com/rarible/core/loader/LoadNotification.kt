package com.rarible.core.loader

import com.rarible.core.loader.internal.common.LoadTaskId

/**
 * Notifications sent to [LoadNotificationListener]s when statuses of corresponding tasks change.
 */
data class LoadNotification(
    val taskId: LoadTaskId,
    val type: LoadType,
    val key: String,
    val status: LoadTaskStatus
)
