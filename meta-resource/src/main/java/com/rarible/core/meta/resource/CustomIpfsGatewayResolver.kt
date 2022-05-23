package com.rarible.core.meta.resource

interface CustomIpfsGatewayResolver {
    fun getResourceLink(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String?
}

class LegacyIpfsGatewaySubstitutor(
    private val legacyGateways: List<String>
) : CustomIpfsGatewayResolver {

    override fun getResourceLink(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String? {
        for (legacy in legacyGateways) {
            if (ipfsUrl.originalGateway == legacy) {
                return "${gateway}/${IpfsUrl.IPFS}${ipfsUrl.path}"
            }
        }
        return null
    }
}
