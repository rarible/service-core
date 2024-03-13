package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.test.ResourceTestData.CID
import com.rarible.core.meta.resource.test.ResourceTestData.ORIGINAL_GATEWAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhitelistIpfsGatewayResolverTest {

    private val parser = UrlParser()
    private val resolver = WhitelistIpfsGatewayResolver(listOf("""https\://ipfs[a-zA-Z0-9_-]+\.quantelica\.com"""))

    @Test
    fun whitelist() {
        val matched1 = parser.parse("https://ipfs-us-private.quantelica.com/ipfs/${CID}") as IpfsUrl
        assertThat(resolver.getResourceUrl(matched1, ORIGINAL_GATEWAY, false))
            .isEqualTo(matched1.original)

        val matched2 = parser.parse("https://ipfs-eu-private.quantelica.com/ipfs/${CID}") as IpfsUrl
        assertThat(resolver.getResourceUrl(matched2, ORIGINAL_GATEWAY, true))
            .isEqualTo(matched2.original)

        val notMatched = parser.parse("https://somegateway.io/ipfs/${CID}") as IpfsUrl
        assertThat(resolver.getResourceUrl(notMatched, ORIGINAL_GATEWAY, false))
            .isNull()
    }
}
