package com.rarible.loader.cache

/**
 * Cache loader event emitted when an entry's state changes.
 */
data class CacheLoaderEvent<T>(
    val type: CacheType,
    val key: String,
    val cacheEntry: CacheEntry<T>
)
