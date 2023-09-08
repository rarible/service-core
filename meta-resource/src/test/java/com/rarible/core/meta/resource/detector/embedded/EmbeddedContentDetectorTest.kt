package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.model.MimeType
import org.apache.commons.codec.binary.Base64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmbeddedContentDetectorTest {

    private val detector = EmbeddedContentDetector()

    @Test
    fun `not an embedded content`() {
        assertThat(detector.detect("abc")).isNull()
        assertThat(detector.detect("http://test.net")).isNull()
        assertThat(detector.detect("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi")).isNull()
        assertThat(detector.detect("QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw")).isNull()
    }

    @Test
    fun `embedded html`() {
        val html = "<html><svg></svg></html>"
        val content = detector.detect(html)!!

        assertThat(content.meta.size).isEqualTo(html.toByteArray().size.toLong())
        assertThat(content.meta.mimeType).isEqualTo("text/html")
    }

    @Test
    fun `embedded base64 html`() {
        val data = "<html><svg></svg></html>".toByteArray()
        val html = "data:text/html;base64," + Base64.encodeBase64String(data)
        val content = detector.detect(html)!!

        assertThat(content.meta.size).isEqualTo(data.size.toLong())
        assertThat(content.meta.mimeType).isEqualTo(MimeType.HTML_TEXT.value)
    }

    @Test
    fun `embedded html with base64 image`() {
        val data = "<html><svg>data:text/html;base64,a</svg></html>"
        val content = detector.detect(data)!!

        assertThat(content.meta.size).isEqualTo(47)
        assertThat(String(content.content)).isEqualTo(data)
        assertThat(content.meta.mimeType).isEqualTo(MimeType.HTML_TEXT.value)
    }

    @Test
    fun `embedded svg`() {
        val svg = "<svg></svg>"
        val content = detector.detect(svg)!!

        assertThat(content.meta.size).isEqualTo(svg.toByteArray().size.toLong())
        assertThat(content.meta.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)
    }

    @Test
    fun `embedded image`() {
        val pixel = "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="
        val content = detector.detect(pixel)!!

        assertThat(content.meta.size).isEqualTo(43)
        assertThat(content.meta.mimeType).isEqualTo(MimeType.GIF_IMAGE.value)
        assertThat(content.meta.width).isEqualTo(1)
        assertThat(content.meta.height).isEqualTo(1)
    }

    @Test
    fun `embedded image - broken mime type`() {
        val pixel = "some useless text;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="
        val content = detector.detect(pixel)!!

        assertThat(content.meta.size).isEqualTo(43)
        assertThat(content.meta.mimeType).isEqualTo(MimeType.GIF_IMAGE.value)
        assertThat(content.meta.width).isEqualTo(1)
        assertThat(content.meta.height).isEqualTo(1)
    }

    @Test
    fun `embedded image - unknown data`() {
        val pixel = "data:image/gif;base64,YWJj"
        val content = detector.detect(pixel)!!

        assertThat(content.meta.size).isEqualTo(3)
        assertThat(content.meta.mimeType).isEqualTo(MimeType.GIF_IMAGE.value) // taken from base64 string
        assertThat(content.meta.width).isNull()
        assertThat(content.meta.height).isNull()
    }
}
