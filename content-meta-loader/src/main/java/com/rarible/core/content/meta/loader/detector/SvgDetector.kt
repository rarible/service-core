package com.rarible.core.content.meta.loader.detector

import com.google.common.primitives.Bytes
import com.rarible.core.content.meta.loader.ContentBytes
import com.rarible.core.content.meta.loader.ContentMeta
import org.slf4j.LoggerFactory

object SvgDetector : ContentMetaDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    private const val SVG_MIME_TYPE_START = "image/svg"
    private const val SVG_MIME_TYPE = "image/svg+xml"

    private val SVG_TAG = "<svg".toByteArray(Charsets.UTF_8) // Tag could contains something like <svg a='b'>

    override fun detect(contentBytes: ContentBytes): ContentMeta? {
        if (!isSvg(contentBytes)) return null

        val url = contentBytes.url

        val result = ContentMeta(
            type = SVG_MIME_TYPE,
            width = 192,
            height = 192,
            size = contentBytes.contentLength
        )

        logger.info("${logPrefix(url)}: parsed SVG content meta $result")

        return result
    }

    private fun isSvg(contentBytes: ContentBytes): Boolean {
        val receivedMimeType = contentBytes.contentType ?: ""
        // Checking content-type (could be like 'image/svg; charset=utf-8') and opening of tag <svg> in the content
        return receivedMimeType.startsWith(SVG_MIME_TYPE_START)
            || Bytes.indexOf(contentBytes.bytes, SVG_TAG) >= 0
    }
}