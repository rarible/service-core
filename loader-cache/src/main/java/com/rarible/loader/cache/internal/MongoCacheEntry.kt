package com.rarible.loader.cache.internal

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class MongoCacheEntry<T>(
    @Id
    val key: String,
    val data: T,
    val cachedAt: Instant,
    @Version
    val version: Long = 0
)
