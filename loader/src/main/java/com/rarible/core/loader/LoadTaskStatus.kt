package com.rarible.core.loader

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

/**
 * Status of a scheduled task.
 */
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "SCHEDULED", value = LoadTaskStatus.Scheduled::class),
    JsonSubTypes.Type(name = "LOADED", value = LoadTaskStatus.Loaded::class),
    JsonSubTypes.Type(name = "WAITS_FOR_RETRY", value = LoadTaskStatus.WaitsForRetry::class),
    JsonSubTypes.Type(name = "FAILED", value = LoadTaskStatus.Failed::class)
)
sealed class LoadTaskStatus {
    /**
     * Timestamp when the task was initially scheduled.
     */
    abstract val scheduledAt: Instant

    /**
     * Number of retry attempts that have already been made to complete this task.
     */
    abstract val retryAttempts: Int

    /**
     * Base marker subclass for [Scheduled] and [WaitsForRetry].
     * These statuses mean that the task has not completed yet.
     */
    sealed class Pending : LoadTaskStatus()

    /**
     * The task has been scheduled but has not completed nor failed yet.
     */
    data class Scheduled(
        override val scheduledAt: Instant
    ) : Pending() {
        override val retryAttempts: Int get() = 0
    }

    /**
     * The task successfully loaded at [loadedAt] timestamp after [retryAttempts] retry attempts.
     */
    data class Loaded(
        override val scheduledAt: Instant,
        val loadedAt: Instant,
        override val retryAttempts: Int
    ) : LoadTaskStatus()

    /**
     * The task failed on the last retry attempt with error ([errorMessage])
     * and is waiting to be retried at [retryAt].
     * The failure happened at [failedAt] with error [errorMessage].
     */
    data class WaitsForRetry(
        override val scheduledAt: Instant,
        val retryAt: Instant,
        override val retryAttempts: Int,
        val failedAt: Instant,
        val errorMessage: String
    ) : Pending()

    /**
     * The task failed several times, and we gave up retrying.
     * The failure happened at [failedAt] with error [errorMessage].
     * There were [retryAttempts] retry attempts.
     */
    data class Failed(
        override val scheduledAt: Instant,
        val failedAt: Instant,
        override val retryAttempts: Int,
        val errorMessage: String
    ) : LoadTaskStatus()
}
