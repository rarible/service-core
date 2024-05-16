package com.rarible.core.meta.resolver

data class MetaResult<M>(
    val meta: M?,
    val metaUrl: String,
    val isMedia: Boolean
)
