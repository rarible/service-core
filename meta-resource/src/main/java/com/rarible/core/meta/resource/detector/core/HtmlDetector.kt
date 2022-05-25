package com.rarible.core.meta.resource.detector.core

import com.google.common.primitives.Bytes
import com.rarible.core.meta.resource.detector.new.MimeType
import org.slf4j.LoggerFactory

object HtmlDetector : ContentMetaDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val HTML_TAG = "<html".toByteArray(Charsets.UTF_8) // Tag could contains something like <html lang="en">

    override fun detect(contentBytes: ContentBytes): ContentMeta? {
        if (!isHtml(contentBytes)) return null

        val url = contentBytes.url

        val result = ContentMeta(
            type = contentBytes.contentType ?: MimeType.HTML_TEXT.value,
            size = contentBytes.contentLength
        )

        logger.info("${logPrefix(url)}: parsed HTML content meta $result")
        return result
    }

    private fun isHtml(contentBytes: ContentBytes): Boolean {
        val receivedMimeType = contentBytes.contentType ?: ""

        // Checking content-type (could be like 'text/html; charset=utf-8') and opening of tag <html> in the content
        return receivedMimeType.startsWith(MimeType.HTML_TEXT.value)
            || Bytes.indexOf(contentBytes.bytes, HTML_TAG) >= 0
    }

}
