package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.SimpleHttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpAddressParserTest {

    private val httpAddressParser = HttpAddressParser()

    @Test
    fun `valid http`() {
        assertThat(httpAddressParser.parse("https://some.io/something")).isEqualTo(
            SimpleHttpUrl(origin = "https://some.io/something")
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
