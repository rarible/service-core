package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.HttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpAddressParserTest {

    private val httpAddressParser = HttpUrlResourceParser()

    @Test
    fun `valid http`() {
        assertThat(httpAddressParser.parse("https://some.io/something")).isEqualTo(
            HttpUrl(original = "https://some.io/something")
        )
    }

    @Test
    fun `invalid URL`() {
        assertThat(httpAddressParser.parse("https//some.io/something")).isNull()
    }

    @Test
    fun `valid URL not http`() {
        assertThat(httpAddressParser.parse("ftp://some.io/something")).isNull()
    }
}
