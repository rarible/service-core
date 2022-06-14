package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.model.MimeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SvgDecoderTest {

    @Test
    fun `regular svg`() {
        val data = "<svg></svg>"
        val decoded = SvgDecoder.decode(data)!!

        assertThat(decoded.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)
        assertThat(decoded.data).isEqualTo(data.toByteArray())
        assertThat(decoded.size).isEqualTo(11L)
    }

    @Test
    fun `corrupted svg`() {
        val data = "http://abc/<svg></svg>/somethingelse"
        val decoded = SvgDecoder.decode(data)!!

        assertThat(decoded.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)
        assertThat(decoded.data).isEqualTo("<svg></svg>".toByteArray())
        assertThat(decoded.size).isEqualTo(11L)
    }

    @Test
    fun `encoded svg`() {
        val data = "%3Csvg%20a%3D2%3E!%3C%2Fsvg%3E"
        val decoded = SvgDecoder.decode(data)!!

        assertThat(decoded.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)
        assertThat(decoded.data).isEqualTo("<svg a=2>!</svg>".toByteArray())
        assertThat(decoded.size).isEqualTo(16L)
    }

    @Test
    fun `encoded corrupted svg`() {
        val data = "http://something%3Csvg%20a%3D2%3E!%3C%2Fsvg%3E/abc"
        val decoded = SvgDecoder.decode(data)!!

        assertThat(decoded.mimeType).isEqualTo(MimeType.SVG_XML_IMAGE.value)
        assertThat(decoded.data).isEqualTo("<svg a=2>!</svg>".toByteArray())
        assertThat(decoded.size).isEqualTo(16L)
    }

    @Test
    fun `svg inside of html`() {
        val data = "<html><svg></svg></html>"
        assertThat(SvgDecoder.decode(data)).isNull()
    }

    @Test
    fun `not a svg`() {
        assertThat(SvgDecoder.decode("<html></html>")).isNull()
        assertThat(SvgDecoder.decode("http://localhost:8080/abc")).isNull()
    }
}