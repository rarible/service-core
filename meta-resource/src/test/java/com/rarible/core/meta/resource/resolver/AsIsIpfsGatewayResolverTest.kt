package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.test.ResourceTestData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AsIsIpfsGatewayResolverTest {

    private val parser = UrlParser()
    private val resolver = AsIsIpfsGatewayResolver(listOf("""https\://ipfs[a-zA-Z0-9_-]+\.quantelica\.com"""))

    @Test
    fun getResourceUrl() {
        val matched1 = parser.parse("https://ipfs-us-private.quantelica.com/ipfs/${ResourceTestData.CID}") as IpfsUrl
        assertThat(resolver.getResourceUrl(matched1, ResourceTestData.ORIGINAL_GATEWAY, false))
            .isEqualTo(matched1.original)

        val matched2 = parser.parse("https://ipfs-eu-private.quantelica.com/ipfs/${ResourceTestData.CID}") as IpfsUrl
        assertThat(resolver.getResourceUrl(matched2, ResourceTestData.ORIGINAL_GATEWAY, true))
            .isEqualTo(matched2.original)

        val notMatched = parser.parse("https://somegateway.io/ipfs/${ResourceTestData.CID}") as IpfsUrl
        assertThat(resolver.getResourceUrl(notMatched, ResourceTestData.ORIGINAL_GATEWAY, false))
            .isNull()
    }
}
