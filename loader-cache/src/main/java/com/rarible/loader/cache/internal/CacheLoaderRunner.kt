package com.rarible.loader.cache.internal

import com.rarible.core.loader.Loader
import com.rarible.core.loader.internal.common.nowMillis
import com.rarible.loader.cache.CacheLoader
import com.rarible.loader.cache.CacheType
import org.slf4j.LoggerFactory
import java.time.Clock

class CacheLoaderRunner<T>(
    private val cacheLoader: CacheLoader<T>,
    private val repository: CacheRepository,
    private val clock: Clock
) : Loader {

    private val logger = LoggerFactory.getLogger(CacheLoaderRunner::class.java)
    private val cacheType: CacheType = cacheLoader.type

    override val type = encodeLoadType(cacheType)

    override suspend fun load(key: String) {
        logger.info("Loading cache value of '$cacheType' for key '$key'")
        val data = try {
            cacheLoader.load(key)
        } catch (e: Exception) {
            logger.info("Failed to load cache value of '$cacheType' for key '$key'", e)
            throw e
        }

        val exist = repository.get<T>(cacheType, key)?.data
        val updated = exist?.let { cacheLoader.update(data, it) } ?: data

        repository.save(cacheType, key, updated, clock.nowMillis())

        if (updated != data) {
            logger.info("Saved loaded and modified cache value of '$cacheType' for key '$key': $data -> $updated")
        } else {
            logger.info("Saved loaded cache value of '$cacheType' for key '$key'")
        }
    }
}
