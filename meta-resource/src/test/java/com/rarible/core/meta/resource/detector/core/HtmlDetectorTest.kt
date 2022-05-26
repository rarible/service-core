package com.rarible.core.meta.resource.detector.core

import com.rarible.core.meta.resource.detector.ContentBytes
import com.rarible.core.meta.resource.detector.ContentMeta
import com.rarible.core.meta.resource.detector.MimeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HtmlDetectorTest {

    @Test
    fun `not a html`() {
        val bytesWithoutMimeType = ContentBytes.EMPTY.copy(bytes = "<svg>abc</svg>".toByteArray())
        val bytesWithMimeType = ContentBytes.EMPTY.copy(
            bytes = "<svg>abc</svg>".toByteArray(), contentType = "text/svg"
        )

        assertNull(HtmlDetector.detect(bytesWithoutMimeType))
        assertNull(HtmlDetector.detect(bytesWithMimeType))
    }

    @Test
    fun `html by mime type`() {
        val htmlType = ContentBytes.EMPTY.copy(contentType = MimeType.HTML_TEXT.value)
        val htmlTypeWithEncoding = ContentBytes.EMPTY.copy(contentType = "${MimeType.HTML_TEXT.value}; charset=utf-8")

        val htmlContent = HtmlDetector.detect(htmlType)
        val htmlContentWithEncoding = HtmlDetector.detect(htmlTypeWithEncoding)

        assertEquals(ContentMeta(type = MimeType.HTML_TEXT.value), htmlContent)
        assertEquals(ContentMeta(type = "${MimeType.HTML_TEXT.value}; charset=utf-8"), htmlContentWithEncoding)
    }

    @Test
    fun `html by content`() {
        val withoutMimeType = ContentBytes.EMPTY.copy(
            contentType = null, bytes = "abc<html>".toByteArray(), contentLength = 50
        )
        val withMimeType = ContentBytes.EMPTY.copy(
            contentType = "${MimeType.HTML_TEXT.value}; charset=utf-8", bytes = "<html lang='en'>".toByteArray()
        )

        val htmlContent = HtmlDetector.detect(withoutMimeType)
        val htmlContentWithMimeType = HtmlDetector.detect(withMimeType)

        assertEquals(ContentMeta(type = MimeType.HTML_TEXT.value, size = 50), htmlContent)
        assertEquals(ContentMeta(type = "${MimeType.HTML_TEXT.value}; charset=utf-8"), htmlContentWithMimeType)
    }

}
