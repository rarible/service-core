package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.model.IpfsUrl

interface CustomIpfsGatewayResolver {

    fun getResourceUrl(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String?
}

class CompositeCustomIpfsGatewayResolver(
    private val resolvers: List<CustomIpfsGatewayResolver>
) : CustomIpfsGatewayResolver {

    override fun getResourceUrl(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String? {
        return resolvers.firstNotNullOfOrNull {
            it.getResourceUrl(ipfsUrl, gateway, replaceOriginalHost)
        }
    }
}

// Fully substitute default logic of IPFS URLs resolution:
// If gateway in whitelist - use it 'as is' in any case
// Otherwise - always substitute original gateway by provided one
class WhitelistIpfsGatewayResolver(
    whitelist: List<String>,
) : CustomIpfsGatewayResolver {

    private val patterns = whitelist
        .filter { it.isNotBlank() }
        .map { it.toRegex() }

    override fun getResourceUrl(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String? {
        for (pattern in patterns) {
            if (ipfsUrl.originalGateway?.matches(pattern) == true) {
                return ipfsUrl.original
            }
        }
        return "$gateway/${IpfsUrl.IPFS}/${ipfsUrl.path}"
    }
}
