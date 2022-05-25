package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.detector.MimeType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EmbeddedSvgDetector : EmbeddedContentDetector {

    override fun canDecode(url: String): Boolean {
        return getPrefixIndex(url) >= 0
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

    override fun getMimeType(url: String): String {
        return MimeType.SVG_IMAGE.value
    }

    override fun getDecodedData(url: String): ByteArray? {
        return getData(url).toByteArray()
    }

    override fun getEmbeddedContent(url: String): EmbeddedContent? {
        if (canDecode(url)) {
            return EmbeddedContent(
                mimyType = getMimeType(url),
                content = getDecodedData(url)
            )
        }
        return null
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(EmbeddedSvgDetector::class.java)
        private const val svgTag = "<svg"
        const val spaceCode = "%20"

        private fun getPrefixIndex(url: String) = url.indexOf(svgTag)
    }
}
