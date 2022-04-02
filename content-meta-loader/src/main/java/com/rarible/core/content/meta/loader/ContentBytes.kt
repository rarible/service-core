package com.rarible.core.content.meta.loader

@Suppress("ArrayInDataClass")
data class ContentBytes(
    val bytes: ByteArray,
    val contentType: String?,
    val contentLength: Long?
)
