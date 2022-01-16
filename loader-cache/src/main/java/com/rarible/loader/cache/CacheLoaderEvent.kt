package com.rarible.loader.cache

/**
 * Cache loader event emitted when an entry's state changes.
 */
data class CacheLoaderEvent<T>(
    /**
     * Cache type.
     */
    val type: CacheType,
    /**
     * Primary key of the cache entry.
     */
    val key: String,
    /**
     * Cache entry payload and metadata.
     */
    val cacheEntry: CacheEntry<T>
)
