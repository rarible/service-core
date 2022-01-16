package com.rarible.loader.cache.internal

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.loader.cache.CacheLoaderServiceRegistry
import com.rarible.loader.cache.CacheType

class CacheLoaderServiceRegistryImpl(
    private val cacheLoaderServices: List<CacheLoaderService<*>>
) : CacheLoaderServiceRegistry {

    override val types: List<CacheType>
        get() = cacheLoaderServices.map { it.type }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCacheLoaderService(type: CacheType): CacheLoaderService<T> {
        return cacheLoaderServices.find { it.type == type } as? CacheLoaderService<T>
            ?: throw AssertionError("No cache loader service found for $type")
    }
}
