package com.rarible.loader.cache.internal

import com.rarible.core.loader.LoadService
import com.rarible.core.loader.LoadTaskStatus
import com.rarible.core.loader.generateLoadTaskId
import com.rarible.core.loader.internal.common.nowMillis
import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.CacheType
import java.time.Clock

class CacheLoaderServiceImpl<T>(
    override val type: CacheType,
    private val cacheRepository: CacheRepository,
    private val loadService: LoadService,
    private val cacheLoadTaskIdService: CacheLoadTaskIdService,
    private val clock: Clock
) : CacheLoaderService<T> {

    override suspend fun update(key: String) {
        val loadTaskId = generateLoadTaskId()
        cacheLoadTaskIdService.save(type, key, loadTaskId)
        loadService.scheduleLoad(
            loadType = encodeLoadType(type),
            key = key,
            loadTaskId = loadTaskId
        )
    }

    override suspend fun remove(key: String) {
        cacheRepository.remove(type, key)
        cacheLoadTaskIdService.remove(type, key)
    }

    override suspend fun save(key: String, data: T) {
        cacheRepository.save(type, key, data, clock.nowMillis())
    }

    override suspend fun get(key: String): CacheEntry<T> {
        val cacheEntry = cacheRepository.get<T>(type, key)
        if (cacheEntry != null) {
            return toLoadedCacheEntry(cacheEntry)
        }
        return getFromTask(key)
    }

    override suspend fun getAll(keys: List<String>): List<CacheEntry<T>> {
        val entries = cacheRepository.getAll<T>(type, keys)
        val byKey = entries.associateByTo(HashMap(), { it.key }, { toLoadedCacheEntry(it) })
        // Keep same order
        return keys.map { byKey[it] ?: getFromTask(it) }
    }

    // TODO it's fine to use this method per single key if cache already filled,
    // but in general we need some kind of batching here
    private suspend fun getFromTask(key: String): CacheEntry<T> {
        val loadTaskId = cacheLoadTaskIdService.getLastTaskId(type, key)
        return when (val loadStatus = loadTaskId?.let { loadService.getStatus(loadTaskId) }) {
            is LoadTaskStatus.Scheduled -> getInitialLoading(key, loadStatus)
            is LoadTaskStatus.WaitsForRetry -> getInitialLoading(key, loadStatus)
            is LoadTaskStatus.Failed -> getInitialFailed(key, loadStatus)
            null -> getNotAvailable(key) // Hasn't been scheduled and not available.
            is LoadTaskStatus.Loaded -> getNotAvailable(key) // Removed entry.
        }
    }

    private fun toLoadedCacheEntry(mongoCacheEntry: MongoCacheEntry<T>): CacheEntry<T> {
        return CacheEntry.Loaded(
            key = mongoCacheEntry.key,
            cachedAt = mongoCacheEntry.cachedAt,
            data = mongoCacheEntry.data
        )
    }

    override suspend fun getAvailable(key: String): T? =
        when (val cacheEntry = get(key)) {
            is CacheEntry.Loaded -> cacheEntry.data
            is CacheEntry.LoadedAndUpdateFailed -> cacheEntry.data
            is CacheEntry.LoadedAndUpdateScheduled -> cacheEntry.data
            is CacheEntry.InitialFailed -> null
            is CacheEntry.InitialLoadScheduled -> null
            is CacheEntry.NotAvailable -> null
        }

    @Suppress("UNCHECKED_CAST")
    private fun getInitialLoading(key: String, pendingLoadStatus: LoadTaskStatus.Pending): CacheEntry<T> =
        CacheEntry.InitialLoadScheduled(key, pendingLoadStatus)

    @Suppress("UNCHECKED_CAST")
    private fun getInitialFailed(key: String, failedTaskStatus: LoadTaskStatus.Failed): CacheEntry<T> =
        CacheEntry.InitialFailed(key, failedTaskStatus)

    @Suppress("UNCHECKED_CAST")
    private fun getNotAvailable(key: String): CacheEntry<T> = CacheEntry.NotAvailable(key)
}
