package com.rarible.core.loader.internal

import com.rarible.core.loader.LoadType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class LoadTask(
    @Id
    val id: LoadTaskId,
    val type: LoadType,
    val key: String,
    val status: Status,
    @Version
    val version: Long = 0
) {

    sealed class Status {
        abstract val scheduledAt: Instant

        abstract val retryAttempts: Int

        data class Scheduled(
            override val scheduledAt: Instant
        ) : Status() {
            override val retryAttempts: Int get() = 0
        }

        data class WaitsForRetry(
            override val scheduledAt: Instant,
            override val retryAttempts: Int,
            val retryAt: Instant,
            val failedAt: Instant,
            val errorMessage: String,
            val rescheduled: Boolean
        ) : Status()

        data class Loaded(
            override val scheduledAt: Instant,
            override val retryAttempts: Int,
            val loadedAt: Instant
        ) : Status()

        data class Failed(
            override val scheduledAt: Instant,
            override val retryAttempts: Int,
            val failedAt: Instant,
            val errorMessage: String
        ) : Status()
    }
}
