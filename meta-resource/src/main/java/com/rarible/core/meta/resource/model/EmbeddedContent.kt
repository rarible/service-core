package com.rarible.core.meta.resource.model

@Suppress("ArrayInDataClass")
data class EmbeddedContent(
    val content: ByteArray,
    val meta: ContentMeta
)
