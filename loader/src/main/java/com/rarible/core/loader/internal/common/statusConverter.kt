package com.rarible.core.loader.internal.common

import com.rarible.core.loader.LoadTaskStatus

fun LoadTask.Status.toApiStatus() = when (this) {
    is LoadTask.Status.Scheduled -> LoadTaskStatus.Scheduled(
        scheduledAt = scheduledAt
    )
    is LoadTask.Status.Loaded -> LoadTaskStatus.Loaded(
        scheduledAt = scheduledAt,
        loadedAt = loadedAt,
        retryAttempts = retryAttempts
    )
    is LoadTask.Status.Failed -> LoadTaskStatus.Failed(
        scheduledAt = failedAt,
        retryAttempts = retryAttempts,
        failedAt = failedAt,
        errorMessage = errorMessage
    )
    is LoadTask.Status.WaitsForRetry -> LoadTaskStatus.WaitsForRetry(
        scheduledAt = scheduledAt,
        retryAttempts = retryAttempts,
        failedAt = failedAt,
        errorMessage = errorMessage,
        retryAt = retryAt
    )
}
