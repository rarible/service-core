package com.rarible.core.content.meta.loader.addressing.resolver

import com.rarible.core.content.meta.loader.addressing.IpfsUrl
import com.rarible.core.content.meta.loader.addressing.SLASH
import com.rarible.core.content.meta.loader.addressing.GatewayProvider
import com.rarible.core.content.meta.loader.addressing.IpfsUrl.Companion.IPFS
import com.rarible.core.content.meta.loader.addressing.isValidUrl

class IpfsGatewayResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val innerGatewaysProvider: GatewayProvider,
    private val customGatewaysResolver: GatewayProvider
) : GatewayResolver<IpfsUrl> {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    override fun resolveInnerAddress(address: IpfsUrl): String =
        resolveInternal(
            address = address,
            gateway = innerGatewaysProvider.getGateway(),
            replaceOriginalHost = true // For internal calls original IPFS host should be replaced in order to avoid rate limit of the original gateway
        )

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    override fun resolvePublicAddress(address: IpfsUrl): String =
        resolveInternal(
            address = address,
            gateway = publicGatewayProvider.getGateway(),
            replaceOriginalHost = false // For public IPFS urls we want to keep original gateway URL (if possible)
        )

    private fun resolveInternal(address: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String {
        // If there is IPFS URL with one of legacy gateways, we need to replace it with actual public gateway
        for (legacy in customGatewaysResolver.getAllGateways()) {
            if (address.originalGateway == legacy) {
                return resolveWithGateway(address, gateway)
            }
        }

        // If URL is valid, and we want to keep original IPFS gateway, return 'as is'
        if (!replaceOriginalHost && address.origin.isValidUrl()) {
            return address.origin
        }

        return resolveWithGateway(address, gateway)
    }

    private fun resolveWithGateway(url: IpfsUrl, gateway: String): String = "$gateway$SLASH$IPFS${url.path}"
}
