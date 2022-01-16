package com.rarible.core.loader

/**
 * Loader interface to be implemented by clients.
 *
 * There must be exactly one `@Component` `Loader` for each unique [type] registered in the application.
 */
interface Loader {
    /**
     * Unique type of [Loader] with which the loader is registered in the application.
     */
    val type: LoadType

    /**
     * Loader logic to be executed.
     *
     * This method is allowed to throw an `Exception`, in which case
     * the task is retried multiple times with configured retry policy.
     */
    suspend fun load(key: String)
}
