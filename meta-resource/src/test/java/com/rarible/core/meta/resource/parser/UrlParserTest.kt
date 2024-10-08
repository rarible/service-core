package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.IpfsUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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

    @ParameterizedTest
    @MethodSource("ipfsParseTest")
    fun `parse ipfs url`(testCase: IpfsParseTestCase) {
        // given
        val url = testCase.ipfsUrl

        // when
        val parsed = urlParser.parse(url)

        // then
        assertThat(parsed).isNotNull
        assertThat(parsed).isExactlyInstanceOf(IpfsUrl::class.java)
        parsed as IpfsUrl
        assertThat(parsed.toSchemaUrl()).isEqualTo(testCase.expectedUrl)
    }

    companion object {
        @JvmStatic
        fun ipfsParseTest(): List<IpfsParseTestCase> = listOf(
            IpfsParseTestCase(
                ipfsUrl = "/ipfs://QmRifoET5PjzDWBe6QXovu4RX6bzMqcURKwN24dQjxiusH",
                expectedUrl = "ipfs://QmRifoET5PjzDWBe6QXovu4RX6bzMqcURKwN24dQjxiusH"
            ),
            IpfsParseTestCase(
                ipfsUrl = "/ipfs/QmRifoET5PjzDWBe6QXovu4RX6bzMqcURKwN24dQjxiusH",
                expectedUrl = "ipfs://QmRifoET5PjzDWBe6QXovu4RX6bzMqcURKwN24dQjxiusH"
            )
        )
    }

    data class IpfsParseTestCase(
        val ipfsUrl: String,
        val expectedUrl: String,
    )
}
