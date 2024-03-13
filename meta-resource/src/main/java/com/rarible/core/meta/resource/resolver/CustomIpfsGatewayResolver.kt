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

class LegacyIpfsGatewaySubstitutor(
    private val legacyGateways: List<String>
) : CustomIpfsGatewayResolver {

    override fun getResourceUrl(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String? {
        for (legacy in legacyGateways) {
            if (ipfsUrl.originalGateway == legacy) {
                return "$gateway/${IpfsUrl.IPFS}/${ipfsUrl.path}"
            }
        }
        return null
    }
}

class WhitelistIpfsGatewayResolver(
    whitelist: List<String>,
) : CustomIpfsGatewayResolver {

    private val patterns = whitelist
        .filter { it.isNotBlank() }
        .map { it.toRegex() }

    override fun getResourceUrl(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String? {
        for (pattern in patterns) {
            if (ipfsUrl.originalGateway?.matches(pattern) == true) {
                return null
            }
        }
        return "$gateway/${IpfsUrl.IPFS}/${ipfsUrl.path}"
    }
}

class AsIsIpfsGatewayResolver(
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
        return null
    }
}
