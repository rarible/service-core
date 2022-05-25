package com.rarible.core.meta.resource.detector.embedded

import org.apache.commons.codec.binary.Base64

/**
 * Parser/Detector for URLs in meta like "https://rarible.mypinata.cloud/data:image/png;base64,iVBORw0KGgoAAAANS..."
 */
class EmbeddedBase64Detector : EmbeddedContentDetector {

    override fun canDecode(url: String): Boolean {
        return getMarkerIndex(url) >= 0
    }

    override fun getData(url: String): String {
        val prefixIndex = url.indexOf(BASE_64_MARKER)
        return url.substring(prefixIndex + BASE_64_MARKER.length).trim()
    }

    override fun getDecodedData(url: String): ByteArray? {
        return Base64.decodeBase64(getData(url))
    }


    override fun getMimeType(url: String): String {
        return url.substring(url.indexOf(MIME_TYPE_PREFIX) + MIME_TYPE_PREFIX.length, getMarkerIndex(url)).trim()
    }

    override fun getEmbeddedContent(url: String): EmbeddedContent? {
        if (canDecode(url)) {
            return EmbeddedContent(
                mimyType = getMimeType(url),
                content = getDecodedData(url)
            )
        }
        return null
    }

    // Don't want to use regex here, not sure how fast it will work on large strings
    companion object {
        private const val MIME_TYPE_PREFIX = "data:"
        private const val BASE_64_MARKER = ";base64,"

        private fun getMarkerIndex(url: String): Int = url.indexOf(BASE_64_MARKER)
    }
}

