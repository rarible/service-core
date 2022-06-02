package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.IpfsUrl
import com.rarible.core.meta.resource.test.ResourceTestData.CID
import com.rarible.core.meta.resource.test.ResourceTestData.IPFS_CUSTOM_GATEWAY
import com.rarible.core.meta.resource.test.ResourceTestData.IPFS_PRIVATE_GATEWAY
import com.rarible.core.meta.resource.test.ResourceTestData.IPFS_PUBLIC_GATEWAY
import com.rarible.core.meta.resource.test.ResourceTestData.ORIGINAL_GATEWAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IpfsGatewayResolverTest {

    private val ipfsGatewayResolver = IpfsGatewayResolver(
        publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
        internalGatewayProvider = RandomGatewayProvider(listOf(IPFS_PRIVATE_GATEWAY)),
        customGatewaysResolver = LegacyIpfsGatewaySubstitutor(listOf(IPFS_CUSTOM_GATEWAY))
    )

    @Test
    fun `resolve public and originalGateway is valid http`() {
        val url = ipfsGatewayResolver.resolveUrl(
            resource = IpfsUrl(
                original = "$ORIGINAL_GATEWAY/ipfs/$CID",
                originalGateway = ORIGINAL_GATEWAY,
                path = CID
            ),
            isPublic = true
        )
        assertThat(url).isEqualTo("$ORIGINAL_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `resolve public and originalGateway is not valid http`() {
        val url = ipfsGatewayResolver.resolveUrl(
            resource = IpfsUrl(
                original = "ipfs://ipfs/$CID",
                originalGateway = null,
                path = CID
            ),
            isPublic = true
        )
        assertThat(url).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `resolve internal and originalGateway is valid http`() {
        val url = ipfsGatewayResolver.resolveUrl(
            resource = IpfsUrl(
                original = "$ORIGINAL_GATEWAY/ipfs/$CID",
                originalGateway = ORIGINAL_GATEWAY,
                path = CID
            ),
            isPublic = false
        )
        assertThat(url).isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `resolve internal and originalGateway is not valid http`() {
        val url = ipfsGatewayResolver.resolveUrl(
            resource = IpfsUrl(
                original = "ipfs://ipfs/$CID",
                originalGateway = null,
                path = CID
            ),
            isPublic = false
        )
        assertThat(url).isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID")
    }
}
