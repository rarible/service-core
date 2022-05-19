package com.rarible.core.content.meta.loader.ipfs

class IpfsUrlResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val innerGatewaysResolver: GatewayProvider,
    private val customGatewaysResolver: GatewayProvider,
) {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    fun resolveInnerHttpUrl(url: MetaUrl): String =
        resolveInternal(
            url = url,
            gateway = innerGatewaysResolver.getGateway(),
            replaceOriginalHost = true // For internal calls original IPFS host should be replaced in order to avoid rate limit of the original gateway
        )

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    fun resolvePublicHttpUrl(url: MetaUrl): String =
        resolveInternal(
            url = url,
            gateway = publicGatewayProvider.getGateway(),
            replaceOriginalHost = false // For public IPFS urls we want to keep original gateway URL (if possible)
        )

    private fun resolveInternal(
        url: MetaUrl,
        gateway: String,
        replaceOriginalHost: Boolean
    ): String =
        when (url) {
            is SimpleHttpUrl -> url.getSourceUrl()
            is IpfsUrl -> resolveIpfs(url, gateway, replaceOriginalHost)
            else -> throw UnsupportedOperationException("Unsupported resolving for ${url.javaClass.name}")
        }

    private fun resolveIpfs(url: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String {
        // If there is IPFS URL with one of legacy gateways, we need to replace it with actual public gateway
        for (legacy in customGatewaysResolver.getAllGateways()) {
            if (url.getSourceUrl().startsWith(legacy)) {   // TODO Maybe change to url.gateway.equals?
                return gateway + url.ipfsPath
            }
        }

        // If URL is valid, and we want to keep original IPFS gateway, return 'as is'
        if (!replaceOriginalHost && url.getSourceUrl().isValidUrl()) {
            return url.getSourceUrl()
        }

        return url.resolveWithGateway(gateway)
    }
}
