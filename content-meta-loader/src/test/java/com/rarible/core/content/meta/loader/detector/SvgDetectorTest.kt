package com.rarible.core.content.meta.loader.detector

import com.rarible.core.content.meta.loader.ContentBytes
import com.rarible.core.content.meta.loader.ContentMeta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SvgDetectorTest {

    @Test
    fun `not a svg`() {
        val bytesWithoutMimeType = ContentBytes.EMPTY.copy(bytes = "<html>abc</html>".toByteArray())
        val bytesWithMimeType = ContentBytes.EMPTY.copy(
            bytes = "<html>abc</html>".toByteArray(), contentType = "text/html"
        )

        assertNull(SvgDetector.detect(bytesWithoutMimeType))
        assertNull(SvgDetector.detect(bytesWithMimeType))
    }

    @Test
    fun `svg by mime type`() {
        val svgType = ContentBytes.EMPTY.copy(contentType = "image/svg+xml")
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
            contentType = "image/svg+xml", bytes = "<svg a='b'>".toByteArray()
        )

        val svgContent = SvgDetector.detect(withoutMimeType)
        val svgContentWithMimeType = SvgDetector.detect(withMimeType)

        assertEquals(expectedSvg(50), svgContent)
        assertEquals(expectedSvg(), svgContentWithMimeType)
    }

    private fun expectedSvg(size: Long? = null): ContentMeta {
        return ContentMeta(
            type = "image/svg+xml",
            width = 192,
            height = 192,
            size = size
        )
    }

}