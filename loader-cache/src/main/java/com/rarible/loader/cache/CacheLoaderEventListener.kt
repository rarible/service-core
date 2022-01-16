package com.rarible.loader.cache

interface CacheLoaderEventListener<T> {
    val type: CacheType

    // TODO[loader]: allow to compare whether the data was changed.
    suspend fun onEvent(cacheLoaderEvent: CacheLoaderEvent<T>)
}
