package com.rarible.core.meta.resource.detector

import java.net.URL

@Suppress("ArrayInDataClass")
data class ContentBytes(
    val url: URL,
    val bytes: ByteArray,
    val contentType: String?,
    val contentLength: Long?
) {

    companion object {

        val EMPTY = ContentBytes(
            URL("http://localhost"),
            ByteArray(0),
            null,
            null
        )
    }

}
