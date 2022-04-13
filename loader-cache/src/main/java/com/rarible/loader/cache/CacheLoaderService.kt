package com.rarible.loader.cache

/**
 * API service to be used by clients to load and cache data-type specific entries.
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
     * Get the cache entry and metadata for entries by list of [keys].
     */
    suspend fun getAll(keys: List<String>): List<CacheEntry<T>>

    /**
     * Schedule an update of a cache entry by [key].
     */
    suspend fun update(key: String)

    /**
     * Save data by [key]. [get] will return [CacheEntry.Loaded] result for it.
     */
    suspend fun save(key: String, data: T)

    /**
     * Remove cache entry by [key].
     */
    suspend fun remove(key: String)

    /**
     * Convenient API to get current cached data.
     * - If the [key] has not yet been scheduled, this method returns `null`.
     * - If the [key] is currently being loaded for the first time or has failed to be loaded, this method returns `null`.
     * - If the [key] is being updated or an update has failed, this method will return previously available data.
     */
    suspend fun getAvailable(key: String): T?

}
