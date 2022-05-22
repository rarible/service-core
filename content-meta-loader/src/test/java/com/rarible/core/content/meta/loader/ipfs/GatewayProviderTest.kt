package com.rarible.core.content.meta.loader.ipfs

import com.rarible.core.content.meta.loader.addressing.ipfs.RandomGatewayProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GatewayProviderTest {

    private val resolver = RandomGatewayProvider(listOf(PREDEFINED_IPFS_GATEWAYS))

    @Test
    fun `resolve predefined gateway`() {
        val gateway = resolver.getGateway()
        assertThat(PREDEFINED_IPFS_GATEWAYS).contains(gateway)
    }

    @Test
    fun `resolve predefined gateways`() {
        val gateways = resolver.getAllGateways()
        assertThat(gateways).isEqualTo(listOf(GATEWAY_ONE, GATEWAY_TWO))
    }

    companion object {
        private const val GATEWAY_ONE = "https://ipfs1.io"
        private const val GATEWAY_TWO = "https://ipfs2.io"
        private const val GATEWAY_THREE = "https://ipfs3.io"
        private const val GATEWAY_FOUR = "https://ipfs4.io"
        private const val PREDEFINED_IPFS_GATEWAYS = "$GATEWAY_ONE, $GATEWAY_TWO"
    }
}
