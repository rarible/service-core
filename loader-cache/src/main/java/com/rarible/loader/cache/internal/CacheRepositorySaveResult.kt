package com.rarible.loader.cache.internal

data class CacheRepositorySaveResult<T>(
    val key: String,
    val previousData: T?,
    val newData: T
)
