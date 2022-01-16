package com.rarible.core.loader

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "SCHEDULED", value = LoadTaskStatus.Scheduled::class),
    JsonSubTypes.Type(name = "LOADED", value = LoadTaskStatus.Loaded::class),
    JsonSubTypes.Type(name = "WAITS_FOR_RETRY", value = LoadTaskStatus.WaitsForRetry::class),
    JsonSubTypes.Type(name = "FAILED", value = LoadTaskStatus.Failed::class)
)
sealed class LoadTaskStatus {
    abstract val scheduledAt: Instant

    abstract val retryAttempts: Int

    sealed class Pending : LoadTaskStatus()

    data class Scheduled(
        override val scheduledAt: Instant
    ) : Pending() {
        override val retryAttempts: Int get() = 0
    }

    data class Loaded(
        override val scheduledAt: Instant,
        val loadedAt: Instant,
        override val retryAttempts: Int
    ) : LoadTaskStatus()

    data class WaitsForRetry(
        override val scheduledAt: Instant,
        val retryAt: Instant,
        override val retryAttempts: Int,
        val failedAt: Instant,
        val errorMessage: String
    ) : Pending()

    data class Failed(
        override val scheduledAt: Instant,
        val failedAt: Instant,
        override val retryAttempts: Int,
        val errorMessage: String
    ) : LoadTaskStatus()
}
