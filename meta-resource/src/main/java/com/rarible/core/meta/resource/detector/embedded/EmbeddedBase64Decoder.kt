package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.detector.extractDecodedData
import com.rarible.core.meta.resource.detector.extractMimeType
import com.rarible.core.meta.resource.detector.getMarkerIndex

/**
 * Parser/Detector for URLs in meta like "https://rarible.mypinata.cloud/data:image/png;base64,iVBORw0KGgoAAAANS..."
 */
object EmbeddedBase64Decoder : EmbeddedContentDecoder {

    override fun isDetected(url: String): Boolean {
        return getMarkerIndex(url) >= 0
    }

    override fun getEmbeddedContent(url: String): EmbeddedContent? {
        if (!isDetected(url)) return null

        return EmbeddedContent(
            mimyType = extractMimeType(url),
            content = extractDecodedData(url)
        )
    }
}

