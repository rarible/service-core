package com.rarible.loader.cache

import com.rarible.core.loader.LoadTaskStatus
import java.time.Instant

sealed class CacheEntry<T> {
    data class Loaded<T>(
        val cachedAt: Instant,
        val data: T
    ) : CacheEntry<T>()

    data class LoadedAndUpdateScheduled<T>(
        val cachedAt: Instant,
        val data: T,
        val updateStatus: LoadTaskStatus.Pending
    ) : CacheEntry<T>()

    data class LoadedAndUpdateFailed<T>(
        val cachedAt: Instant,
        val data: T,
        val failedUpdateStatus: LoadTaskStatus.Failed
    ) : CacheEntry<T>()

    data class InitialLoadScheduled<T>(
        val loadStatus: LoadTaskStatus.Pending
    ) : CacheEntry<T>()

    data class InitialFailed<T>(
        val failedStatus: LoadTaskStatus.Failed
    ) : CacheEntry<T>()

    class NotAvailable<T> : CacheEntry<T>() {
        override fun hashCode(): Int = 0
        override fun equals(other: Any?): Boolean = other is NotAvailable<*>
    }
}
