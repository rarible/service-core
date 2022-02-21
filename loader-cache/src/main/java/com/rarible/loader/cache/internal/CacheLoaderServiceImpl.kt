package com.rarible.loader.cache.internal

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.LoadService
import com.rarible.core.loader.LoadTaskStatus
import com.rarible.core.loader.generateLoadTaskId
import com.rarible.core.loader.internal.common.nowMillis
import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.CacheType
import org.slf4j.LoggerFactory
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
    }

    override suspend fun save(key: String, data: T) {
        cacheRepository.save(type, key, data, clock.nowMillis())
    }

    override suspend fun get(key: String): CacheEntry<T> {
        val loadTaskId = cacheLoadTaskIdService.getLastTaskId(type, key)
        val loadStatus = loadTaskId?.let { loadService.getStatus(loadTaskId) }
        val cacheEntry = cacheRepository.get<T>(type, key)
            ?: return when (loadStatus) {
                is LoadTaskStatus.Scheduled -> getInitialLoading(loadStatus)
                is LoadTaskStatus.WaitsForRetry -> getInitialLoading(loadStatus)
                is LoadTaskStatus.Failed -> getInitialFailed(loadStatus)
                null -> getNotAvailable() // Hasn't been scheduled and not available.
                is LoadTaskStatus.Loaded -> getNotAvailable() // Removed entry.
            }
        return getAvailableCacheEntry(cacheEntry, loadStatus)
    }

    private fun getAvailableCacheEntry(
        cacheEntry: MongoCacheEntry<T>,
        loadStatus: LoadTaskStatus?
    ) = when (loadStatus) {
        null, is LoadTaskStatus.Loaded -> CacheEntry.Loaded(
            cachedAt = cacheEntry.cachedAt,
            data = cacheEntry.data
        )
        is LoadTaskStatus.Failed -> CacheEntry.LoadedAndUpdateFailed(
            cachedAt = cacheEntry.cachedAt,
            data = cacheEntry.data,
            failedUpdateStatus = loadStatus
        )
        is LoadTaskStatus.Scheduled -> CacheEntry.LoadedAndUpdateScheduled(
            cachedAt = cacheEntry.cachedAt,
            data = cacheEntry.data,
            updateStatus = loadStatus
        )
        is LoadTaskStatus.WaitsForRetry -> CacheEntry.LoadedAndUpdateScheduled(
            cachedAt = cacheEntry.cachedAt,
            data = cacheEntry.data,
            updateStatus = loadStatus
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
    private fun getInitialLoading(pendingLoadStatus: LoadTaskStatus.Pending): CacheEntry<T> =
        CacheEntry.InitialLoadScheduled(pendingLoadStatus)

    @Suppress("UNCHECKED_CAST")
    private fun getInitialFailed(failedTaskStatus: LoadTaskStatus.Failed): CacheEntry<T> =
        CacheEntry.InitialFailed(failedTaskStatus)

    @Suppress("UNCHECKED_CAST")
    private fun getNotAvailable(): CacheEntry<T> = CacheEntry.NotAvailable()
}
