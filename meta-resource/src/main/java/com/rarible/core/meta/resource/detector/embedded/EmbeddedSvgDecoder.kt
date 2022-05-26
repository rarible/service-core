package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.detector.MimeType
import com.rarible.core.meta.resource.detector.SPACE_CODE
import com.rarible.core.meta.resource.detector.SVG_TAG
import com.rarible.core.meta.resource.detector.containsSvgTag
import org.slf4j.LoggerFactory

object EmbeddedSvgDecoder : EmbeddedContentDecoder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun isDetected(url: String): Boolean {
        return containsSvgTag(url)
    }

    override fun getEmbeddedContent(url: String): EmbeddedContent? {
        if (!isDetected(url)) return null

        return EmbeddedContent(
            mimyType = MimeType.SVG_XML_IMAGE.value,
            content = getData(url).toByteArray()
        )
    }

    private fun getData(url: String): String =
        url
            .workaroundBravo1872()
            .extractSvg()
            .fixCorruptedSvg()

    private fun String.workaroundBravo1872(): String {
        return if (SPACE_CODE in this) {
            logger.warn("Broken svg: $this")
            this.replace(SPACE_CODE, " ") //TODO Workaround for BRAVO-1872.
        } else {
            this
        }
    }

    private fun String.extractSvg(): String {
        return this.substring(this.indexOf(SVG_TAG), this.length)
    }

    private fun String.fixCorruptedSvg(): String {
        return this.replace(
            // TODO what if 'fill: %23'?
            "fill:%23",
            "fill:#"
        )
    }
}
