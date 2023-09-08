package com.rarible.core.meta.resource.detector

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HtmlDetectorTest {

    @Test
    fun `not a html`() {
        val bytesWithoutMimeType = ContentData.EMPTY.copy(data = "<svg>abc</svg>".toByteArray())
        val bytesWithMimeType = ContentData.EMPTY.copy(
            data = "<svg>abc</svg>".toByteArray(), mimeType = "text/svg"
        )

        assertNull(HtmlDetector.detect(bytesWithoutMimeType, ""))
        assertNull(HtmlDetector.detect(bytesWithMimeType, ""))
    }

    @Test
    fun `html by mime type`() {
        val htmlType = ContentData.EMPTY.copy(mimeType = MimeType.HTML_TEXT.value)
        val htmlTypeWithEncoding = ContentData.EMPTY.copy(mimeType = "${MimeType.HTML_TEXT.value}; charset=utf-8")

        val htmlContent = HtmlDetector.detect(htmlType, "")
        val htmlContentWithEncoding = HtmlDetector.detect(htmlTypeWithEncoding, "")

        assertEquals(ContentMeta(mimeType = MimeType.HTML_TEXT.value), htmlContent)
        assertEquals(ContentMeta(mimeType = "${MimeType.HTML_TEXT.value}; charset=utf-8"), htmlContentWithEncoding)
    }

    @Test
    fun `html by content`() {
        val withoutMimeType = ContentData.EMPTY.copy(
            mimeType = null, data = "abc<html>".toByteArray(), size = 50
        )
        val withMimeType = ContentData.EMPTY.copy(
            mimeType = "${MimeType.HTML_TEXT.value}; charset=utf-8", data = "<html lang='en'>".toByteArray()
        )

        val htmlContent = HtmlDetector.detect(withoutMimeType, "")
        val htmlContentWithMimeType = HtmlDetector.detect(withMimeType, "")

        assertEquals(ContentMeta(mimeType = MimeType.HTML_TEXT.value, size = 50), htmlContent)
        assertEquals(ContentMeta(mimeType = "${MimeType.HTML_TEXT.value}; charset=utf-8"), htmlContentWithMimeType)
    }
}
