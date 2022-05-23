package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.CustomIpfsGatewayResolver
import com.rarible.core.meta.resource.GatewayProvider
import com.rarible.core.meta.resource.IpfsUrl
import com.rarible.core.meta.resource.IpfsUrl.Companion.IPFS
import com.rarible.core.meta.resource.isHttpUrl

class IpfsGatewayResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val innerGatewaysProvider: GatewayProvider,
    private val customGatewaysResolver: CustomIpfsGatewayResolver
) : GatewayResolver<IpfsUrl> {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    override fun resolveInnerAddress(ipfsUrl: IpfsUrl): String =
        resolveInternal(
            ipfsUrl = ipfsUrl,
            gateway = innerGatewaysProvider.getGateway(),
            replaceOriginalHost = true // For internal calls original IPFS host should be replaced in order to avoid rate limit of the original gateway
        )

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    override fun resolvePublicAddress(address: IpfsUrl): String =
        resolveInternal(
            ipfsUrl = address,
            gateway = publicGatewayProvider.getGateway(),
            replaceOriginalHost = false // For public IPFS urls we want to keep original gateway URL (if possible)
        )

    private fun resolveInternal(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String {
        // If there is IPFS URL with one of legacy gateways, we need to replace it with actual public gateway
        customGatewaysResolver.getUrlResource(ipfsUrl, gateway, replaceOriginalHost)?.let { return it }

        // If URL is valid, and we want to keep original IPFS gateway, return 'as is'
        if (!replaceOriginalHost && ipfsUrl.original.isHttpUrl()) {
            return ipfsUrl.original
        }

        return resolveWithGateway(ipfsUrl, gateway)
    }

    private fun resolveWithGateway(url: IpfsUrl, gateway: String): String = "$gateway/$IPFS${url.path}"
}
