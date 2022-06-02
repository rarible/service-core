package com.rarible.core.meta.resource.detector

import com.google.common.primitives.Bytes
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import org.slf4j.LoggerFactory

object SvgDetector : MediaDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    private const val SVG_TAG = "<svg"
    private val SVG_TAG_BYTES = SVG_TAG.toByteArray(Charsets.UTF_8) // Tag could contains something like <svg a='b'>

    private const val SVG_MIME_TYPE_START = "image/svg"

    override fun detect(contentBytes: ContentData, entityId: String): ContentMeta? {
        if (!isSvg(contentBytes)) return null

        return ContentMeta(
            mimeType = MimeType.SVG_XML_IMAGE.value,
            width = 192,
            height = 192,
            size = contentBytes.size
        ).also {
            logger.info("${logPrefix(entityId)}: parsed SVG content meta $it")
        }
    }

    private fun isSvg(contentData: ContentData): Boolean {
        val receivedMimeType = contentData.mimeType ?: ""
        // Checking content-type (could be like 'image/svg; charset=utf-8') and opening of tag <svg> in the content
        return receivedMimeType.startsWith(SVG_MIME_TYPE_START)
            || Bytes.indexOf(contentData.data, SVG_TAG_BYTES) >= 0
    }
}
