package com.rarible.core.meta.resource.detector.ethereum

import com.rarible.core.meta.resource.detector.MimeType
import com.rarible.core.meta.resource.detector.embedded.EmbeddedBase64Decoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmbeddedBase64DetectorTest {

    private val detector = EmbeddedBase64Decoder

    @Test
    fun `is base64 url`() {
        assertThat(detector.isDetected(BASE_64)).isTrue
        assertThat(detector.isDetected("https://some.data.com/ipfs/abc/image.png")).isFalse
    }

    @Test
    fun `get base64 image parts`() {
        val content = detector.getEmbeddedContent(BASE_64)
        assertThat(content?.mimyType).isEqualTo(MimeType.PNG_IMAGE.value)
        assertThat(content?.content?.decodeToString()).isEqualTo("abc")
    }

    @Test
    fun `get base64 image test text type`() {
        val content = detector.getEmbeddedContent(BASE_64_TEXT_TYPE)
        assertThat(content?.mimyType).isEqualTo(MimeType.HTML_TEXT.value)
        assertThat(content?.content?.decodeToString()).isEqualTo("abc")
    }

    companion object {
        private const val BASE_64 = "https://some.data.com/data:image/png;base64,YWJj"
        private const val BASE_64_TEXT_TYPE = "https://some.data.com/data:text/html;base64,YWJj"
    }
}
