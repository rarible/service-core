package com.rarible.loader.cache.internal

import com.rarible.core.loader.LoadType
import com.rarible.loader.cache.CacheType

private const val CACHE_LOADER_PREFIX = "cache-loader-"
fun encodeLoadType(cacheType: CacheType): LoadType = "$CACHE_LOADER_PREFIX$cacheType"
fun decodeLoadType(loadType: LoadType): CacheType = loadType.substringAfter(CACHE_LOADER_PREFIX)
