package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.model.IpfsUrl.Companion.IPFS
import com.rarible.core.meta.resource.util.isHttpUrl

class IpfsGatewayResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val internalGatewayProvider: GatewayProvider,
    private val customGatewaysResolver: CustomIpfsGatewayResolver
) {

    fun resolveUrl(resource: IpfsUrl, isPublic: Boolean): String =
        if (isPublic) {
            resolveInternal(
                ipfsUrl = resource,
                gateway = publicGatewayProvider.getGateway(),
                replaceOriginalHost = false // For public IPFS urls we want to keep original gateway URL (if possible)
            )
        } else {
            resolveInternal(
                ipfsUrl = resource,
                gateway = internalGatewayProvider.getGateway(),
                replaceOriginalHost = true // For internal calls original IPFS host should be replaced in order to avoid rate limit of the original gateway
            )
        }

    private fun resolveInternal(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String {
        // If there is IPFS URL with one of legacy gateways, we need to replace it with actual public gateway
        customGatewaysResolver.getResourceUrl(ipfsUrl, gateway, replaceOriginalHost)?.let { return it }

        // If URL is valid, and we want to keep original IPFS gateway, return 'as is'
        return if (!replaceOriginalHost && ipfsUrl.original.isHttpUrl()) {
            ipfsUrl.original
        } else {
            "$gateway/$IPFS/${ipfsUrl.path}"
        }
    }
}
