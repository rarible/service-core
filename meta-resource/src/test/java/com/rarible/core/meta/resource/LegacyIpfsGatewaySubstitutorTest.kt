package com.rarible.core.meta.resource

import com.rarible.core.meta.resource.ResourceTestData.CID
import com.rarible.core.meta.resource.ResourceTestData.IPFS_CUSTOM_GATEWAY
import com.rarible.core.meta.resource.ResourceTestData.IPFS_PUBLIC_GATEWAY
import com.rarible.core.meta.resource.ResourceTestData.ORIGINAL_GATEWAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LegacyIpfsGatewaySubstitutorTest {

    private val legacyIpfsGatewaySubstitutor = LegacyIpfsGatewaySubstitutor(listOf(IPFS_CUSTOM_GATEWAY))

    @Test
    fun `originalGateway is legacy`() {
        val url = legacyIpfsGatewaySubstitutor.getResourceUrl(
            ipfsUrl = IpfsUrl(
                original = "$IPFS_CUSTOM_GATEWAY/ipfs/$CID",
                originalGateway = IPFS_CUSTOM_GATEWAY,
                path = CID
            ),
            gateway = IPFS_PUBLIC_GATEWAY,
            replaceOriginalHost = true
        )
        assertThat(url).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `originalGateway is not legacy`() {
        assertThat(
            legacyIpfsGatewaySubstitutor.getResourceUrl(
                ipfsUrl = IpfsUrl(
                    original = "$ORIGINAL_GATEWAY/ipfs/$CID",
                    originalGateway = ORIGINAL_GATEWAY,
                    path = CID
                ),
                gateway = IPFS_PUBLIC_GATEWAY,
                replaceOriginalHost = true
            )
        ).isNull()
    }
}
