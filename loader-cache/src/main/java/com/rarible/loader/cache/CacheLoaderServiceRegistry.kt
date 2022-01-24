package com.rarible.loader.cache

/**
 * API registry to obtain implementations of [CacheLoaderService] for specific data types.
 */
interface CacheLoaderServiceRegistry {
    /**
     * All cache types registered in the application.
     */
    val types: List<CacheType>

    /**
     * Get cache loader service instance used for loading cache entries of type [type].
     */
    fun <T> getCacheLoaderService(type: CacheType): CacheLoaderService<T>
}
