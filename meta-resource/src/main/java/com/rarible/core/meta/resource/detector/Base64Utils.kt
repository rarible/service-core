package com.rarible.core.meta.resource.detector

import org.apache.commons.codec.binary.Base64

object Base64Utils {
    // Don't want to use regex here, not sure how fast it will work on large strings
    private const val MIME_TYPE_PREFIX = "data:"
    private const val BASE_64_MARKER = ";base64,"

    fun getMarkerIndex(url: String) = url.indexOf(BASE_64_MARKER)

    fun containsBase64Marker(url: String) = getMarkerIndex(url) >= 0

    fun extractMimeType(url: String): String {
        val startIndex = url.indexOf(MIME_TYPE_PREFIX) + MIME_TYPE_PREFIX.length
        return url.substring(
            startIndex = startIndex,
            endIndex = getMarkerIndex(url)
        ).trim()
    }

    fun extractEncodedData(url: String) = url.substring(getMarkerIndex(url) + BASE_64_MARKER.length).trim()

    fun extractDecodedData(url: String): ByteArray = Base64.decodeBase64(extractEncodedData(url))

    fun base64MimeToBytes(data: String): ByteArray = java.util.Base64.getMimeDecoder().decode(data.toByteArray()) // TODO Change?
}
