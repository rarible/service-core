package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.detector.Base64Utils

/**
 * Parser/Detector for URLs in meta like "https://rarible.mypinata.cloud/data:image/png;base64,iVBORw0KGgoAAAANS..."
 */
object EmbeddedBase64Decoder : EmbeddedContentDecoder {

    override fun isDetected(url: String): Boolean {
        return Base64Utils.containsBase64Marker(url)
    }

    override fun getEmbeddedContent(url: String): EmbeddedContent? {
        if (!isDetected(url)) return null

        return EmbeddedContent(
            mimyType = Base64Utils.extractMimeType(url),
            content = Base64Utils.extractDecodedData(url)
        )
    }
}

