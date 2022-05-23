package com.rarible.core.content.meta.loader.addressing

class LegacyIpfsGatewaySubstitutor(
    private val legacyGateways: List<String>
) : CustomIpfsGatewayResolver {

    override fun getUrlResource(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String? {
        for (legacy in legacyGateways) {
            if (ipfsUrl.originalGateway == legacy) {
                return "${gateway}/${IpfsUrl.IPFS}${ipfsUrl.path}"
            }
        }
        return null
    }
}