package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.detector.MimeType
import org.slf4j.LoggerFactory

object EmbeddedSvgDetector : EmbeddedContentDetector {

//    private val svgDetector: SvgDetector = SvgDetector.detect()

    private val logger = LoggerFactory.getLogger(javaClass)
    private const val svgTag = "<svg"
    private const val spaceCode = "%20"

    override fun isDetected(url: String): Boolean {
        return getPrefixIndex(url) >= 0
    }

    override fun getEmbeddedContent(url: String): EmbeddedContent? {
        if (isDetected(url)) {
            return EmbeddedContent(
                mimyType = MimeType.SVG_XML_IMAGE.value,
                content = getDecodedData(url)
            )
        }
        return null
    }
    override fun getMimeType(url: String): String {
        return MimeType.SVG_XML_IMAGE.value
    }


    override fun getDecodedData(url: String): ByteArray? {
        return getData(url).toByteArray()
    }

    override fun getData(url: String): String {
        val content = if (spaceCode in url) {
            logger.warn("Broken svg: %s", url)
            val decodedData = url.replace(spaceCode, " ")
            //TODO Workaround for BRAVO-1872.
            decodedData.substring(decodedData.indexOf(svgTag), decodedData.length)
        } else {
            url
        }
        // Fix for corrupted SVG
        return content.replace(
            // TODO what if 'fill: %23'?
            "fill:%23",
            "fill:#"
        )
    }

    private fun getPrefixIndex(url: String) = url.indexOf(svgTag)
}
