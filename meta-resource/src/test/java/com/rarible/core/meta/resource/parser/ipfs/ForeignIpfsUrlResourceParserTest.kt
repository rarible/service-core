package com.rarible.core.meta.resource.parser.ipfs

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.test.ResourceTestData.CID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForeignIpfsUrlResourceParserTest {

    private val foreignIpfsUrlResourceParser = ForeignIpfsUrlResourceParser()

    @Test
    fun `Regular IPFS URL`() {
        assertThat(foreignIpfsUrlResourceParser.parse("https://ipfs.io/ipfs/$CID"))
            .isEqualTo(
                IpfsUrl(
                    original = "https://ipfs.io/ipfs/$CID",
                    originalGateway = "https://ipfs.io",
                    path = CID
                )
            )
    }

    @Test
    fun `Regular IPFS URL with 2 ipfs parts`() {
        assertThat(foreignIpfsUrlResourceParser.parse("https://ipfs.io/ipfs/something/ipfs/$CID"))
            .isEqualTo(
                IpfsUrl(
                    original = "https://ipfs.io/ipfs/something/ipfs/$CID",
                    originalGateway = "https://ipfs.io/ipfs/something",
                    path = CID
                )
            )
    }

    @Test
    fun `Regular IPFS URL but without CID`() {
        assertThat(foreignIpfsUrlResourceParser.parse("http://ipfs.io/ipfs/123.jpg")).isNull()
    }

    @Test
    fun `URL but without ipfs part`() {
        assertThat(foreignIpfsUrlResourceParser.parse("http://rarible.io/123.jpg")).isNull()
    }
}
