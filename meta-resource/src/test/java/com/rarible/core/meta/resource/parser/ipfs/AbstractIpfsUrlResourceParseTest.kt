package com.rarible.core.meta.resource.parser.ipfs

import com.rarible.core.meta.resource.IpfsUrl
import com.rarible.core.meta.resource.test.ResourceTestData.CID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AbstractIpfsUrlResourceParseTest {

    private val abstractIpfsUrlResourceParser = AbstractIpfsUrlResourceParser()

    @Test
    fun `IPFS urls with ipfs path and broken slashes`() {
        // Abstract IPFS urls with /ipfs/ path and broken slashes
        assertIpfsUrl("ipfs:/ipfs/${CID}", CID)
        assertIpfsUrl("ipfs://ipfs/${CID}", CID)
        assertIpfsUrl("ipfs:///ipfs/${CID}", CID)
        assertIpfsUrl("ipfs:////ipfs/${CID}", CID)

        assertIpfsUrl("ipfs:////ipfs/${CID}", CID)
        assertIpfsUrl("ipfs:////ipfs//${CID}", CID)
        assertIpfsUrl("ipfs:////ipfs///${CID}", CID)
    }

    @Test
    fun `IPFS urls is too short`() {
        assertThat(abstractIpfsUrlResourceParser.parse("ip")).isNull()
    }

    @Test
    fun `IPFS prefix not found`() {
        assertThat(abstractIpfsUrlResourceParser.parse("ipffs:/ipfs/${CID}")).isNull()
    }

    @Test
    fun `prefixed ipfs urls`() {
        //        assertFixedIpfsUrl("ipfs:/folder/$CID/abc .json", "folder/$CID/abc%20.json")  //SPACE
        assertIpfsUrl("ipfs://folder/abc", "folder/abc")
        assertIpfsUrl("ipfs:///folder/subfolder/$CID", "folder/subfolder/$CID")
        assertIpfsUrl("ipfs:////$CID", CID)

        // Various case of ipfs prefix
        assertIpfsUrl("IPFS://$CID", CID)
        assertIpfsUrl("Ipfs:///$CID", CID)

        // Abstract IPFS urls with /ipfs/ path and broken slashes without a CID
        assertIpfsUrl("ipfs:/ipfs/abc", "abc")
        assertIpfsUrl("ipfs://ipfs/folder/abc", "folder/abc")
        assertIpfsUrl("ipfs:///ipfs/abc", "abc")
    }

    private fun assertIpfsUrl(url: String, expectedPath: String) {
        val result = abstractIpfsUrlResourceParser.parse(url)
        assertThat(result).isEqualTo(
            IpfsUrl(
                original = url,
                originalGateway = null,
                path = expectedPath
            )
        )
    }
}
