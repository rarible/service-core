package com.rarible.loader.cache.internal

import com.rarible.core.loader.LoadService
import com.rarible.core.loader.LoadTaskStatus
import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderEvent
import com.rarible.loader.cache.CacheLoaderEventListener
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.CacheType
import org.slf4j.LoggerFactory

class CacheLoaderServiceImpl<T>(
    override val type: CacheType,
    private val cacheRepository: CacheRepository,
    private val loadService: LoadService,
    private val cacheLoadTaskIdService: CacheLoadTaskIdService,
    private val cacheLoaderEventListener: CacheLoaderEventListener<T>
) : CacheLoaderService<T> {

    private val logger = LoggerFactory.getLogger(CacheLoaderServiceImpl::class.java)

    override suspend fun update(key: String) {
        val loadTaskId = loadService.scheduleLoad(
            loadType = encodeLoadType(type),
            key = key
        )
        cacheLoadTaskIdService.save(type, key, loadTaskId)
    }

    override suspend fun remove(key: String) {
        cacheRepository.remove(type, key)
        cacheLoaderEventListener.onEvent(
            CacheLoaderEvent(
                type = type,
                key = key,
                cacheEntry = getNotAvailable()
            )
        )
    }

    override suspend fun get(key: String): CacheEntry<T> {
        val mongoCacheEntry = cacheRepository.get<T>(type, key)
        val loadTaskId = cacheLoadTaskIdService.getLastTaskId(type, key)
        val loadStatus = loadTaskId?.let { loadService.getStatus(loadTaskId) }
        if (loadStatus == null) {
            if (mongoCacheEntry != null) {
                logger.error("Alert! Cache entry $mongoCacheEntry was loaded without an associated task ($loadTaskId)")
            }
            return getNotAvailable()
        }
        if (mongoCacheEntry == null) {
            return when (loadStatus) {
                is LoadTaskStatus.Failed -> getInitialFailed(loadStatus)
                is LoadTaskStatus.Scheduled -> getInitialLoading(loadStatus)
                is LoadTaskStatus.WaitsForRetry -> getInitialLoading(loadStatus)
                is LoadTaskStatus.Loaded -> {
                    // Apparently, is has just been loaded (concurrent race), request from the cache again.
                    val recentCacheEntry = cacheRepository.get<T>(type, key)
                    if (recentCacheEntry == null) {
                        logger.error("Alert! Task $loadTaskId (${loadStatus}) has been loaded but there is no cache entry for $type $key")
                        getNotAvailable()
                    } else {
                        getAvailableCacheEntry(recentCacheEntry, loadStatus)
                    }
                }
                null -> getNotAvailable()
            }
        }
        return getAvailableCacheEntry(mongoCacheEntry, loadStatus)
    }

    private fun getAvailableCacheEntry(
        mongoCacheEntry: MongoCacheEntry<T>,
        loadStatus: LoadTaskStatus
    ) = when (loadStatus) {
        is LoadTaskStatus.Loaded -> CacheEntry.Loaded(
            cachedAt = mongoCacheEntry.cachedAt,
            data = mongoCacheEntry.data
        )
        is LoadTaskStatus.Failed -> CacheEntry.LoadedAndUpdateFailed(
            cachedAt = mongoCacheEntry.cachedAt,
            data = mongoCacheEntry.data,
            failedUpdateStatus = loadStatus
        )
        is LoadTaskStatus.Scheduled -> CacheEntry.LoadedAndUpdateScheduled(
            cachedAt = mongoCacheEntry.cachedAt,
            data = mongoCacheEntry.data,
            updateStatus = loadStatus
        )
        is LoadTaskStatus.WaitsForRetry -> CacheEntry.LoadedAndUpdateScheduled(
            cachedAt = mongoCacheEntry.cachedAt,
            data = mongoCacheEntry.data,
            updateStatus = loadStatus
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun getInitialLoading(pendingLoadStatus: LoadTaskStatus.Pending): CacheEntry<T> =
        CacheEntry.InitialLoadScheduled<T>(pendingLoadStatus)

    @Suppress("UNCHECKED_CAST")
    private fun getInitialFailed(failedTaskStatus: LoadTaskStatus.Failed): CacheEntry<T> =
        CacheEntry.InitialFailed(failedTaskStatus)

    @Suppress("UNCHECKED_CAST")
    private fun getNotAvailable(): CacheEntry<T> = CacheEntry.NotAvailable()
}
