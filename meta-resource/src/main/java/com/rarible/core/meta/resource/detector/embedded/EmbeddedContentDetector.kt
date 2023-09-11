package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.EmbeddedContent
import kotlin.math.min

class EmbeddedContentDetector(
    private val contentDetector: ContentDetector = ContentDetector(),
    private val contentDecoders: List<ContentDecoder> = listOf(Base64Decoder, SvgDecoder)
) {

    fun detect(data: String): EmbeddedContent? {
        val decoded = contentDecoders.firstNotNullOfOrNull { it.decode(data) }
        val contentData = decoded ?: ContentData(data.toByteArray())

        val entityId = "embedded: ${data.substring(0, min(32, data.length))}"
        val contentMeta = contentDetector.detect(contentData, entityId)

        // Ok, we found embedded content
        contentMeta?.let { return EmbeddedContent(contentData.data, contentMeta) }

        // Otherwise, if we see decoded embedded data, we return what we can gather
        decoded?.let {
            // Originally, there should not be cases when base64 doesn't contain mime type
            val mimeType = decoded.mimeType ?: "text/plain"
            val fallback = ContentMeta(mimeType = mimeType, size = decoded.data.size.toLong())
            return EmbeddedContent(decoded.data, fallback)
        }

        return null
    }
}
