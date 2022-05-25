package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.ArweaveUrl.Companion.ARWEAVE_GATEWAY
import com.rarible.core.meta.resource.ArweaveUrl.Companion.AR_PREFIX
import com.rarible.core.meta.resource.Cid
import com.rarible.core.meta.resource.ConstantGatewayProvider
import com.rarible.core.meta.resource.HttpUrl
import com.rarible.core.meta.resource.RandomGatewayProvider
import com.rarible.core.meta.resource.ResourceTestData.CID
import com.rarible.core.meta.resource.ResourceTestData.IPFS_PRIVATE_GATEWAY
import com.rarible.core.meta.resource.ResourceTestData.IPFS_PUBLIC_GATEWAY
import com.rarible.core.meta.resource.ResourceTestData.ORIGINAL_GATEWAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GatewayResolverTest {

    private val simpleHttpGatewayResolver = SimpleHttpGatewayResolver()
    private val ipfsCidGatewayResolver = IpfsCidGatewayResolver(
        publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
        innerGatewaysProvider = RandomGatewayProvider(listOf(IPFS_PRIVATE_GATEWAY)),
    )
    private val arweaveGatewayResolver = ArweaveGatewayResolver(
        arweaveGatewayProvider = ConstantGatewayProvider(ARWEAVE_GATEWAY)
    )

    @Test
    fun `SimpleHttpGatewayResolver happy path`() {
        assertThat(
            simpleHttpGatewayResolver.resolveLink(
                resource = HttpUrl(original = "$ORIGINAL_GATEWAY/ipfs/${CID}"),
                isPublic = false
            )
        ).isEqualTo("$ORIGINAL_GATEWAY/ipfs/${CID}")
    }

    @Test
    fun `ArweaveGatewayResolver happy path`() {
        assertThat(
            arweaveGatewayResolver.resolveLink(
                resource = ArweaveUrl(
                    original = "$ARWEAVE_GATEWAY/123",
                    originalGateway = ARWEAVE_GATEWAY,
                    path = "/123"
                ),
                isPublic = false
            )
        ).isEqualTo("$ARWEAVE_GATEWAY/123")
    }

    @Test
    fun `ArweaveGatewayResolver originalGateway is null`() {
        assertThat(
            arweaveGatewayResolver.resolveLink(
                resource = ArweaveUrl(
                    original = "${AR_PREFIX}123",
                    originalGateway = null,
                    path = "/123"
                ),
                isPublic = false
            )
        ).isEqualTo("$ARWEAVE_GATEWAY/123")
    }

    @Test
    fun `RawCidGatewayResolver resolve public without additional path`() {
        assertThat(
            ipfsCidGatewayResolver.resolveLink(
                resource = Cid(
                    original = CID,
                    cid = CID,
                    subPath = null
                ),
                isPublic = true
            )
        ).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `RawCidGatewayResolver resolve public with additional path`() {
        assertThat(
            ipfsCidGatewayResolver.resolveLink(
                resource = Cid(
                    original = CID,
                    cid = CID,
                    subPath = "/5032.json"
                ),
                isPublic = true
            )
        ).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID/5032.json")
    }

    @Test
    fun `RawCidGatewayResolver resolve inner without additional path`() {
        assertThat(
            ipfsCidGatewayResolver.resolveLink(
                resource = Cid(
                    original = CID,
                    cid = CID,
                    subPath = null
                ),
                isPublic = false
            )
        ).isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `RawCidGatewayResolver resolve inner with additional path`() {
        assertThat(
            ipfsCidGatewayResolver.resolveLink(
                resource = Cid(
                    original = CID,
                    cid = CID,
                    subPath = "/5032.json"
                ),
                isPublic = false
            )
        ).isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID/5032.json")
    }
}
