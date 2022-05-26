package com.rarible.core.meta.resource.detector.core

import com.rarible.core.meta.resource.detector.ContentBytes
import com.rarible.core.meta.resource.detector.ContentMeta
import com.rarible.core.meta.resource.detector.MimeType
import com.rarible.core.meta.resource.detector.SvgUtils
import org.slf4j.LoggerFactory

object SvgDetector : ContentMetaDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    private const val SVG_MIME_TYPE_START = "image/svg"

    override fun detect(contentBytes: ContentBytes): ContentMeta? {
        if (!isSvg(contentBytes)) return null

        return ContentMeta(
            type = MimeType.SVG_XML_IMAGE.value,
            width = 192,
            height = 192,
            size = contentBytes.contentLength
        ).also {
            logger.info("${logPrefix(contentBytes.url)}: parsed SVG content meta $it")
        }
    }

    private fun isSvg(contentBytes: ContentBytes): Boolean {
        val receivedMimeType = contentBytes.contentType ?: ""
        // Checking content-type (could be like 'image/svg; charset=utf-8') and opening of tag <svg> in the content
        return receivedMimeType.startsWith(SVG_MIME_TYPE_START) || SvgUtils.containsSvgTag(contentBytes.bytes)
    }
}
