package com.rarible.core.content.meta.loader.addressing.resolver

import com.rarible.core.content.meta.loader.addressing.IpfsUrl
import com.rarible.core.content.meta.loader.addressing.ipfs.GatewayProvider
import com.rarible.core.content.meta.loader.addressing.isValidUrl

class IpfsGatewayResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val innerGatewaysProvider: GatewayProvider,
    private val customGatewaysResolver: GatewayProvider
) : GatewayResolver<IpfsUrl> {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    override fun resolveInnerAddress(url: IpfsUrl): String =
        resolveInternal(
            url = url,
            gateway = innerGatewaysProvider.getGateway(),
            replaceOriginalHost = true // For internal calls original IPFS host should be replaced in order to avoid rate limit of the original gateway
        )

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    override fun resolvePublicAddress(url: IpfsUrl): String =
        resolveInternal(
            url = url,
            gateway = publicGatewayProvider.getGateway(),
            replaceOriginalHost = false // For public IPFS urls we want to keep original gateway URL (if possible)
        )

    fun resolveInternal(url: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String {
        // If there is IPFS URL with one of legacy gateways, we need to replace it with actual public gateway
        for (legacy in customGatewaysResolver.getAllGateways()) {
            if (url.originalGateway == legacy) {
                return gateway + url.path
            }
        }

        // If URL is valid, and we want to keep original IPFS gateway, return 'as is'
        if (!replaceOriginalHost && url.origin.isValidUrl()) {
            return url.origin
        }

        return url.resolveWithGateway(gateway)
    }
}
