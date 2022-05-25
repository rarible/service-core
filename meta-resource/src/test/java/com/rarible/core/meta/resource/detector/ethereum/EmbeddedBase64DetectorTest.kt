package com.rarible.core.meta.resource.detector.ethereum

import com.rarible.core.meta.resource.detector.embedded.EmbeddedBase64Detector
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmbeddedBase64DetectorTest {

    private val embeddedBase64Detector = EmbeddedBase64Detector()

    @Test
    fun `is base64 url`() {
        assertThat(embeddedBase64Detector.canDecode(BASE_64)).isTrue
        assertThat(embeddedBase64Detector.canDecode("https://some.data.com/ipfs/abc/image.png")).isFalse
    }

    @Test
    fun `get base64 image parts`() {
        assertThat(embeddedBase64Detector.getData(BASE_64)).isEqualTo("abc")
        assertThat(embeddedBase64Detector.getMimeType(BASE_64)).isEqualTo("image/png")
    }

    @Test
    fun `get base64 image test text type`() {
        assertThat(embeddedBase64Detector.getData(BASE_64_TEXT_TYPE)).isEqualTo("abc")
        assertThat(embeddedBase64Detector.getMimeType(BASE_64_TEXT_TYPE)).isEqualTo("text/html")
    }

    companion object {
        private const val BASE_64 = "https://some.data.com/data:image/png;base64,abc"
        private const val BASE_64_TEXT_TYPE = "https://some.data.com/data:text/html;base64,abc"
    }
}
