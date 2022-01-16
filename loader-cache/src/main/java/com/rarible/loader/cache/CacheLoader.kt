package com.rarible.loader.cache

interface CacheLoader<T> {
    val type: CacheType

    suspend fun load(key: String): T
}
