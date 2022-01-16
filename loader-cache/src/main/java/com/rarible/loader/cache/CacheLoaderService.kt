package com.rarible.loader.cache

// TODO[loader]: add Javadoc for all classes.
// TODO[loader]: allow getting 'failed' result and not load again.

interface CacheLoaderService<T> {
    val type: CacheType
    suspend fun get(key: String): CacheEntry<T>
    suspend fun update(key: String)
    suspend fun remove(key: String)
}
