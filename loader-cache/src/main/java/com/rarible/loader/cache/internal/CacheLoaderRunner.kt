package com.rarible.loader.cache.internal

import com.rarible.core.loader.Loader
import com.rarible.core.loader.internal.nowMillis
import com.rarible.loader.cache.CacheLoader
import com.rarible.loader.cache.CacheType
import org.slf4j.LoggerFactory
import java.time.Clock

class CacheLoaderRunner<T>(
    private val cacheType: CacheType,
    private val cacheLoader: CacheLoader<T>,
    private val repository: CacheRepository,
    private val clock: Clock
) : Loader {

    private val logger = LoggerFactory.getLogger(CacheLoaderRunner::class.java)

    override val type = encodeLoadType(cacheType)

    override suspend fun load(key: String) {
        logger.info("Loading cache value of '$cacheType' for key '$key'")
        val data = try {
            cacheLoader.load(key)
        } catch (e: Exception) {
            logger.info("Failed to load cache value of '$cacheType' for key '$key'", e)
            throw e
        }
        repository.save(cacheType, key, data, clock.nowMillis())
        logger.info("Saved loaded cache value of '$cacheType' for key '$key'")
    }
}
