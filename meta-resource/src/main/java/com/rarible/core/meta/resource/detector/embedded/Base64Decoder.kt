package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.model.ContentData
import org.apache.commons.codec.binary.Base64

/**
 * Parser/Detector for URLs in meta like "https://rarible.mypinata.cloud/data:image/png;base64,iVBORw0KGgoAAAANS..."
 */
object Base64Decoder : ContentDecoder {

    // Don't want to use regex here, not sure how fast it will work on large strings
    private const val MIME_TYPE_PREFIX = "data:"
    private const val BASE_64_MARKER = ";base64,"

    override fun decode(data: String): ContentData? {
        val markerIndex = data.indexOf(BASE_64_MARKER)
        if (markerIndex < 0) return null

        // Case for embedded images inside HTML
        if (data.indexOf("<") >= 0) return null

        val dataStartIndex = markerIndex + BASE_64_MARKER.length
        val encodedData = data.substring(dataStartIndex).trim()
        val bytes = Base64.decodeBase64(encodedData)

        return ContentData(
            mimeType = getMimeType(data, markerIndex),
            data = bytes,
            size = bytes.size.toLong()
        )
    }

    private fun getMimeType(data: String, markerIndex: Int): String? {
        val mimeTypeIndex = data.indexOf(MIME_TYPE_PREFIX)
        if (mimeTypeIndex < 0 || markerIndex < mimeTypeIndex) {
            return null
        }

        val mimeTypeStartIndex = mimeTypeIndex + MIME_TYPE_PREFIX.length
        return data.substring(mimeTypeStartIndex, markerIndex)
    }
}

