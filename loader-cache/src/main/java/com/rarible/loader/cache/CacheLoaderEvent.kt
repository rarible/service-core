package com.rarible.loader.cache

data class CacheLoaderEvent<T>(
    val type: CacheType,
    val key: String,
    val cacheEntry: CacheEntry<T>
)
