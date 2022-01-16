package com.rarible.loader.cache.internal

import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.LoadNotificationListener
import com.rarible.core.loader.LoadTaskStatus
import com.rarible.core.loader.LoadType
import com.rarible.loader.cache.CacheLoaderEvent
import com.rarible.loader.cache.CacheLoaderEventListener
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.CacheType
import org.slf4j.LoggerFactory

class CacheLoaderNotificationListener<T>(
    cacheType: CacheType,
    private val cacheLoaderEventListener: CacheLoaderEventListener<T>,
    private val cacheLoaderService: CacheLoaderService<T>
) : LoadNotificationListener {

    private val logger = LoggerFactory.getLogger(CacheLoaderNotificationListener::class.java)

    override val type: LoadType = encodeLoadType(cacheType)

    override suspend fun onLoadNotification(loadNotification: LoadNotification) {
        val cacheType = decodeLoadType(loadNotification.type)
        val key = loadNotification.key
        val needToNotify = when (val status = loadNotification.status) {
            is LoadTaskStatus.Loaded -> {
                logger.info("Loaded cache entry of '$cacheType' for key '$key'")
                true
            }
            is LoadTaskStatus.Failed -> {
                logger.warn("Failed to load cache entry of '$cacheType' for key '$key': ${status.errorMessage}")
                true
            }
            is LoadTaskStatus.WaitsForRetry -> {
                logger.warn("Loading of '$cacheType' for key '$key' failed and will be retried, error message: ${status.errorMessage}")
                true
            }
            is LoadTaskStatus.Scheduled -> {
                logger.info("Scheduled task of '$cacheType' for key '$key'")
                false
            }
        }
        if (needToNotify) {
            val cacheEntry = cacheLoaderService.get(key)
            cacheLoaderEventListener.onEvent(
                CacheLoaderEvent(
                    type = cacheType,
                    key = key,
                    cacheEntry = cacheEntry
                )
            )
        }
    }
}
