package com.rarible.core.meta.resource.detector.embedded

interface EmbeddedContentDecoder {
    fun isDetected(url: String): Boolean  // TODO Remove unnecessary methods
//    fun getMimeType(url: String): String
//    fun getDecodedData(url: String): ByteArray?

    fun getEmbeddedContent(url: String) : EmbeddedContent?
}
