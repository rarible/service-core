package com.rarible.core.meta.resource.model

data class ContentMeta(
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val size: Long? = null,
    val available: Boolean = false
)
