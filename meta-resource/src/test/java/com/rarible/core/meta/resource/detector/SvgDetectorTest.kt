package com.rarible.core.meta.resource.detector

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SvgDetectorTest {

    @Test
    fun `not a svg`() {
        val bytesWithoutMimeType = ContentData.EMPTY.copy(data = "<html>abc</html>".toByteArray())
        val bytesWithMimeType = ContentData.EMPTY.copy(
            data = "<html>abc</html>".toByteArray(), mimeType = MimeType.HTML_TEXT.value
        )

        assertNull(SvgDetector.detect(bytesWithoutMimeType, ""))
        assertNull(SvgDetector.detect(bytesWithMimeType, ""))
    }

    @Test
    fun `svg by mime type`() {
        val svgType = ContentData.EMPTY.copy(mimeType = MimeType.SVG_XML_IMAGE.value)
        val svgTypeWithEncoding = ContentData.EMPTY.copy(mimeType = "image/svg; charset=utf-8")

        val svgContent = SvgDetector.detect(svgType, "")
        val svgContentWithEncoding = SvgDetector.detect(svgTypeWithEncoding, "")

        // For SVG images we're using dedicated mimeType
        assertEquals(expectedSvg(), svgContent)
        assertEquals(expectedSvg(), svgContentWithEncoding)
    }

    @Test
    fun `svg by content`() {
        val withoutMimeType = ContentData.EMPTY.copy(
            mimeType = null, data = "abc<svg>".toByteArray(), size = 50
        )
        val withMimeType = ContentData.EMPTY.copy(
            mimeType = MimeType.SVG_XML_IMAGE.value, data = "<svg a='b'>".toByteArray()
        )

        val svgContent = SvgDetector.detect(withoutMimeType, "")
        val svgContentWithMimeType = SvgDetector.detect(withMimeType, "")

        assertEquals(expectedSvg(50), svgContent)
        assertEquals(expectedSvg(), svgContentWithMimeType)
    }

    private fun expectedSvg(size: Long? = null): ContentMeta {
        return ContentMeta(
            mimeType = MimeType.SVG_XML_IMAGE.value,
            width = 192,
            height = 192,
            size = size
        )
    }

}
