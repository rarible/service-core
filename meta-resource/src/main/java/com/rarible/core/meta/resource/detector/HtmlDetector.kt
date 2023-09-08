package com.rarible.core.meta.resource.detector

import com.google.common.primitives.Bytes
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import org.slf4j.LoggerFactory

object HtmlDetector : MediaDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val HTML_TAG = "<html".toByteArray(Charsets.UTF_8) // Tag could contains something like <html lang="en">

    override fun detect(contentBytes: ContentData, entityId: String): ContentMeta? {
        if (!isHtml(contentBytes)) return null

        val result = ContentMeta(
            mimeType = contentBytes.mimeType ?: MimeType.HTML_TEXT.value,
            size = contentBytes.size
        )

        logger.info("${logPrefix(entityId)}: parsed HTML content meta $result")
        return result
    }

    private fun isHtml(contentBytes: ContentData): Boolean {
        val receivedMimeType = contentBytes.mimeType ?: ""

        // Checking content-type (could be like 'text/html; charset=utf-8') and opening of tag <html> in the content
        return receivedMimeType.startsWith(MimeType.HTML_TEXT.value) ||
            Bytes.indexOf(contentBytes.data, HTML_TAG) >= 0
    }
}
