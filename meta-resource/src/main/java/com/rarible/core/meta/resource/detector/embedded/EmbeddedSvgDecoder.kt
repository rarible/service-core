package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.detector.MimeType
import com.rarible.core.meta.resource.detector.SvgUtils

object EmbeddedSvgDecoder : EmbeddedContentDecoder {

    override fun isDetected(url: String): Boolean {
        return SvgUtils.containsSvgTag(url)
    }

    override fun getEmbeddedContent(url: String): EmbeddedContent? {
        if (!isDetected(url)) return null

        return EmbeddedContent(
            mimyType = MimeType.SVG_XML_IMAGE.value,
            content = getData(url).toByteArray()
        )
    }

    private fun getData(url: String): String =
        url.extractSvg()

    private fun String.extractSvg(): String {
        return this.substring(this.indexOf(SvgUtils.SVG_TAG), this.length)
    }
}
