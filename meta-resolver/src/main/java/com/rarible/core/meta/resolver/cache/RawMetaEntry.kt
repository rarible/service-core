package com.rarible.core.meta.resolver.cache

import java.time.Instant

data class RawMetaEntry(
    val url: String,
    val updatedAt: Instant,
    val content: String
)
