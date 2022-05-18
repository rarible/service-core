package com.rarible.core.content.meta.loader.ipfs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GatewayResolverTest {

    private val resolver = RandomGatewayResolver(PREDEFINED_IPFS_GATEWAYS)

    @Test
    fun `resolve predefined gateway`() {
        val gateway = resolver.getGateway()
        assertThat(PREDEFINED_IPFS_GATEWAYS).contains(gateway)
    }

    @Test
    fun `resolve predefined gateways`() {
        val gateways = resolver.getGateways()
        assertThat(gateways).isEqualTo(listOf(GATEWAY_ONE, GATEWAY_TWO))
    }

    @Test
    fun `resolve user gateway`() {
        val userGateways = listOf(GATEWAY_THREE, GATEWAY_FOUR)
        val gateway = resolver.getGateway(userGateways)
        assertThat(userGateways).contains(gateway)
    }

    @Test
    fun `resolve user gateways`() {
        val userGateways = listOf(GATEWAY_THREE, GATEWAY_FOUR)
        val gateways = resolver.getGateways(userGateways)
        assertThat(gateways).isEqualTo(userGateways)
    }

    companion object {
        private const val GATEWAY_ONE = "https://ipfs1.io"
        private const val GATEWAY_TWO = "https://ipfs2.io"
        private const val GATEWAY_THREE = "https://ipfs3.io"
        private const val GATEWAY_FOUR = "https://ipfs4.io"
        private const val PREDEFINED_IPFS_GATEWAYS = "$GATEWAY_ONE, $GATEWAY_TWO"
    }
}
