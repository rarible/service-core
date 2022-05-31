package com.rarible.core.meta.resource.detector.core

import com.rarible.core.meta.resource.detector.ContentBytes
import com.rarible.core.meta.resource.detector.ContentMeta
import com.rarible.core.meta.resource.detector.MimeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SvgDetectorTest {

    @Test
    fun `not a svg`() {
        val bytesWithoutMimeType = ContentBytes.EMPTY.copy(bytes = "<html>abc</html>".toByteArray())
        val bytesWithMimeType = ContentBytes.EMPTY.copy(
            bytes = "<html>abc</html>".toByteArray(), contentType = MimeType.HTML_TEXT.value
        )

        assertNull(SvgDetector.detect(bytesWithoutMimeType))
        assertNull(SvgDetector.detect(bytesWithMimeType))
    }

    @Test
    fun `svg by mime type`() {
        val svgType = ContentBytes.EMPTY.copy(contentType = MimeType.SVG_XML_IMAGE.value)
        val svgTypeWithEncoding = ContentBytes.EMPTY.copy(contentType = "image/svg; charset=utf-8")

        val svgContent = SvgDetector.detect(svgType)
        val svgContentWithEncoding = SvgDetector.detect(svgTypeWithEncoding)

        // For SVG images we're using dedicated mimeType
        assertEquals(expectedSvg(), svgContent)
        assertEquals(expectedSvg(), svgContentWithEncoding)
    }

    @Test
    fun `svg by content`() {
        val withoutMimeType = ContentBytes.EMPTY.copy(
            contentType = null, bytes = "abc<svg>".toByteArray(), contentLength = 50
        )
        val withMimeType = ContentBytes.EMPTY.copy(
            contentType = MimeType.SVG_XML_IMAGE.value, bytes = "<svg a='b'>".toByteArray()
        )

        val svgContent = SvgDetector.detect(withoutMimeType)
        val svgContentWithMimeType = SvgDetector.detect(withMimeType)

        assertEquals(expectedSvg(50), svgContent)
        assertEquals(expectedSvg(), svgContentWithMimeType)
    }

    private fun expectedSvg(size: Long? = null): ContentMeta {
        return ContentMeta(
            type = MimeType.SVG_XML_IMAGE.value,
            width = 192,
            height = 192,
            size = size
        )
    }

}
