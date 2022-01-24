package com.rarible.loader.cache

/**
 * Cache loader interface to be implemented by clients.
 *
 * There must be exactly one `@Component` `Loader` for each unique [type] registered in the application.
 */
interface CacheLoader<T> {
    /**
     * Unique type of [CacheLoader] with which the loader is registered in the application.
     */
    val type: CacheType

    /**
     * Loader logic to be executed.
     *
     * This method is allowed to throw an `Exception`, in which case
     * the cache loading will be retried multiple times with configured retry policy.
     */
    suspend fun load(key: String): T
}
