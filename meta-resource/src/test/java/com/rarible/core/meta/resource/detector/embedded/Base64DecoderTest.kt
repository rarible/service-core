package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.model.MimeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Base64DecoderTest {

    private val detector = Base64Decoder

    @Test
    fun `is base64 url`() {
        assertThat(detector.decode(BASE_64)).isNotNull()
        assertThat(detector.decode("https://some.data.com/ipfs/abc/image.png")).isNull()
    }

    @Test
    fun `get base64 image parts`() {
        val content = detector.decode(BASE_64)
        assertThat(content?.mimeType).isEqualTo(MimeType.PNG_IMAGE.value)
        assertThat(content?.data?.decodeToString()).isEqualTo("abc")
    }

    @Test
    fun `get base64 image test text type`() {
        val content = detector.decode(BASE_64_TEXT_TYPE)
        assertThat(content?.mimeType).isEqualTo(MimeType.HTML_TEXT.value)
        assertThat(content?.data?.decodeToString()).isEqualTo("abc")
    }

    companion object {

        private const val BASE_64 = "https://some.data.com/data:image/png;base64,YWJj"
        private const val BASE_64_TEXT_TYPE = "https://some.data.com/data:text/html;base64,YWJj"
    }
}
