package com.rarible.core.loader.internal

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
        failedAt = failedAt,
        retryAttempts = retryAttempts,
        errorMessage = errorMessage
    )
    is LoadTask.Status.WaitsForRetry -> LoadTaskStatus.WaitsForRetry(
        scheduledAt = scheduledAt,
        retryAt = retryAt,
        retryAttempts = retryAttempts,
        failedAt = failedAt,
        errorMessage = errorMessage
    )
}