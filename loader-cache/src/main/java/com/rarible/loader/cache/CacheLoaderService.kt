package com.rarible.loader.cache

/**
 * API service to be used by clients to load and cache data-type specific entries.
 *
 * Instances of this class should be obtained via [CacheLoaderServiceRegistry.getCacheLoaderService]
 * with the necessary cache [type].
 */
interface CacheLoaderService<T> {
    /**
     * Cache type associated with the [CacheLoader].
     */
    val type: CacheType

    /**
     * Get the cache entry and metadata for an entry by [key].
     */
    suspend fun get(key: String): CacheEntry<T>

    /**
     * Schedule an update of a cache entry by [key].
     */
    suspend fun update(key: String)

    /**
     * Remove cache entry by [key].
     */
    suspend fun remove(key: String)
}
