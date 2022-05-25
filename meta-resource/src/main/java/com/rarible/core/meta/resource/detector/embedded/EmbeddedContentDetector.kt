package com.rarible.core.meta.resource.detector.embedded

interface EmbeddedContentDetector {
    fun canDecode(url: String): Boolean
    fun getData(url: String): String
    fun getMimeType(url: String): String
    fun getDecodedData(url: String): ByteArray?

    fun getEmbeddedContent(url: String) : EmbeddedContent?
}
