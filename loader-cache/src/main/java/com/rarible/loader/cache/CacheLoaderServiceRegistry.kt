package com.rarible.loader.cache

interface CacheLoaderServiceRegistry {
    val types: List<CacheType>

    fun <T> getCacheLoaderService(type: CacheType): CacheLoaderService<T>
}
