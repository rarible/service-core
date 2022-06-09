package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.MimeType
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

// Checker for corrupted SVG like http://something.com/<svg></svg>
object SvgDecoder : ContentDecoder {

    private const val HTML_TAG = "<html"
    private const val SVG_OPEN_TAG = "<svg"
    private const val SVG_CLOSE_TAG = "</svg>"

    override fun decode(data: String): ContentData? {
        val decoded = try {
            URLDecoder.decode(data, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            data
        }

        // SVG can be a part of HTML, such data should not be considered as corrupted SVG
        if (decoded.contains(HTML_TAG)) {
            return null
        }

        val start = decoded.indexOf(SVG_OPEN_TAG)
        if (start < 0) return null

        val closeTag = decoded.lastIndexOf(SVG_CLOSE_TAG)
        val end = if (closeTag < 0) decoded.length else closeTag + SVG_CLOSE_TAG.length

        val bytes = decoded.substring(start, end).toByteArray()
        return ContentData(
            data = bytes,
            mimeType = MimeType.SVG_XML_IMAGE.value,
            size = bytes.size.toLong()
        )

    }
}