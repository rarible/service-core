package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.test.ResourceTestData.CID
import com.rarible.core.meta.resource.test.ResourceTestData.ORIGINAL_GATEWAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhitelistIpfsGatewayResolverTest {

    private val parser = UrlParser()
    private val resolver = WhitelistIpfsGatewayResolver(listOf("""https\://[a-zA-Z0-9_-]+\.infura-ipfs\.io"""))

    @Test
    fun whitelist() {
        val matched1 = parser.parse("https://ola.infura-ipfs.io/ipfs/$CID") as IpfsUrl
        assertThat(resolver.getResourceUrl(matched1, ORIGINAL_GATEWAY, false)).isNull()

        val matched2 = parser.parse("https://test-url.infura-ipfs.io/ipfs/$CID") as IpfsUrl
        assertThat(resolver.getResourceUrl(matched2, ORIGINAL_GATEWAY, true)).isNull()

        val notMatched = parser.parse("https://somegateway.io/ipfs/$CID") as IpfsUrl
        assertThat(resolver.getResourceUrl(notMatched, ORIGINAL_GATEWAY, false))
            .isEqualTo("$ORIGINAL_GATEWAY/ipfs/$CID")
    }
}
