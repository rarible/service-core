package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.HttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpUrlResourceParserTest {

    private val httpUrlResourceParser = HttpUrlResourceParser()

    @Test
    fun `valid http`() {
        assertThat(httpUrlResourceParser.parse("https://some.io/something")).isEqualTo(
            HttpUrl(original = "https://some.io/something")
        )
    }

    @Test
    fun `invalid URL`() {
        assertThat(httpUrlResourceParser.parse("https//some.io/something")).isNull()
    }

    @Test
    fun `valid URL not http`() {
        assertThat(httpUrlResourceParser.parse("ftp://some.io/something")).isNull()
    }
}
