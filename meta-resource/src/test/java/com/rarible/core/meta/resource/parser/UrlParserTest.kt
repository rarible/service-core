package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.IpfsUrl
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

    @Test
    fun `should parse url`() {
        // given
        val url = "https://cryptocubes.io/api/v1/ipfs/QmbKP6tTL6getrPaoP2j5XPAj3Mgy1LTnZGRfFBYP3N1My"
        val expected = "https://cryptocubes.io/api/v1/ipfs/QmbKP6tTL6getrPaoP2j5XPAj3Mgy1LTnZGRfFBYP3N1My"

        // when
        val parsed = urlParser.parse(url)

        // then
        assertThat(parsed).isNotNull
        assertThat(parsed).isExactlyInstanceOf(HttpUrl::class.java)
        parsed as HttpUrl
        assertThat(parsed.original).isEqualTo(expected)
    }

    @Test
    fun `parse url with leading slash`() {
        // given
        val url = "/ipfs://QmRifoET5PjzDWBe6QXovu4RX6bzMqcURKwN24dQjxiusH"

        // when
        val parsed = urlParser.parse(url)

        // then
        assertThat(parsed).isNotNull
        assertThat(parsed).isExactlyInstanceOf(IpfsUrl::class.java)
        parsed as IpfsUrl
        assertThat(parsed.toSchemaUrl()).isEqualTo("ipfs://QmRifoET5PjzDWBe6QXovu4RX6bzMqcURKwN24dQjxiusH")
    }
}
