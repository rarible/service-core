package com.rarible.core.content.meta.loader.addressing.parser.ipfs

import com.rarible.core.content.meta.loader.addressing.AddressingTestData
import com.rarible.core.content.meta.loader.addressing.IpfsUrl
import com.rarible.core.content.meta.loader.addressing.cid.CidV1Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForeignIpfsUrlAddressParserTest {

    private val cidOneValidator = CidV1Validator()
    private val foreignIpfsUrlAddressParser = ForeignIpfsUrlResourceParser(
        cidOneValidator = cidOneValidator
    )

    @Test
    fun `Regular IPFS URL`() {
        assertIpfsUrl(
            url = "https://ipfs.io/ipfs/${AddressingTestData.CID}",
            originalGateway = "https://ipfs.io",
            path = "/${AddressingTestData.CID}"
        )
    }

    @Test
    fun `Regular IPFS URL with 2 ipfs parts`() {
        assertIpfsUrl(
            url = "https://ipfs.io/ipfs/something/ipfs/${AddressingTestData.CID}",
            originalGateway = "https://ipfs.io/ipfs/something",
            path = "/${AddressingTestData.CID}"
        )
    }

    @Test
    fun `Regular IPFS URL but without CID`() {
        assertThat(foreignIpfsUrlAddressParser.parse("http://ipfs.io/ipfs/123.jpg")).isNull()
    }

    @Test
    fun `URL but without ipfs part`() {
        assertThat(foreignIpfsUrlAddressParser.parse("http://rarible.io/123.jpg")).isNull()
    }

    private fun assertIpfsUrl(url: String, originalGateway: String, path: String) {
        assertThat(foreignIpfsUrlAddressParser.parse(url))
            .isEqualTo(IpfsUrl(
                original = url,
                originalGateway = originalGateway,
                path = path
            ))
    }

}
