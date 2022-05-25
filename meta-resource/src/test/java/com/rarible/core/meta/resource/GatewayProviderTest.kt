package com.rarible.core.meta.resource

import com.rarible.core.meta.resource.ResourceTestData.IPFS_PUBLIC_GATEWAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GatewayProviderTest {

    private val resolver = RandomGatewayProvider(PREDEFINED_IPFS_GATEWAYS)

    @Test
    fun `resolve predefined gateway`() {
        val gateway = resolver.getGateway()
        assertThat(PREDEFINED_IPFS_GATEWAYS).contains(gateway)
    }

    companion object {
        private const val GATEWAY_TWO = "https://ipfs2.io"
        private val PREDEFINED_IPFS_GATEWAYS = listOf(IPFS_PUBLIC_GATEWAY, GATEWAY_TWO)
    }
}
