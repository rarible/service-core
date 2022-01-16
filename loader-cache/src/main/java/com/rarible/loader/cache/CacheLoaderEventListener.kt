package com.rarible.loader.cache

/**
 * API interface to be registered as a `@Component` bean
 * to receive cache loader [events][CacheLoaderEvent]
 */
interface CacheLoaderEventListener<T> {
    /**
     * The type of cache events this listener wants to listen to.
     */
    val type: CacheType

    /**
     * Callback invoked on receiving a cache loader event.
     */
    suspend fun onEvent(cacheLoaderEvent: CacheLoaderEvent<T>)
}
