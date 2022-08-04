package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.model.HttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UrlParserTest {

    private val urlParser = UrlParser()

    @Test
    fun `should parse url enclosed in quotes`() {
        // given
        val url = "\"https://gateway.pinata.cloud/ipfs/Qmbo0008guW9BoVCw96UUs0003VStp8SvaHVz70009FdaU\""
        val expected = "https://gateway.pinata.cloud/ipfs/Qmbo0008guW9BoVCw96UUs0003VStp8SvaHVz70009FdaU"

        // when
        val parsed = urlParser.parse(url)

        // then
        assertThat(parsed).isNotNull
        assertThat(parsed).isExactlyInstanceOf(HttpUrl::class.java)
        parsed as HttpUrl
        assertThat(parsed.original).isEqualTo(expected)
    }
}