package com.rarible.core.content.meta.loader.ipfs

import com.rarible.core.content.meta.loader.ipfs.checker.AbstractIpfsUrlChecker
import com.rarible.core.content.meta.loader.ipfs.checker.EmbeddedImageChecker
import com.rarible.core.content.meta.loader.ipfs.checker.ForeignerIpfsUriChecker
import org.springframework.stereotype.Component

@Component
class IpfsUrlResolver(
    val publicGatewayResolver: GatewayResolver,
    val innerGatewaysResolver: GatewayResolver,
    val embeddedImageChecker: EmbeddedImageChecker,
    val foreignerIpfsUriChecker: ForeignerIpfsUriChecker,
    val abstractIpfsUrlChecker: AbstractIpfsUrlChecker
) {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     * @param innerGateways for override inner gateways list
     * @param customGateways for override custom gateways list
     */
    fun resolveInnerHttpUrl(
        url: String,
        innerGateways: List<String> = emptyList(),
        customGateways: List<String> = emptyList()
    ): String {
        return resolveHttpUrl(
            url = url,
            gateway = innerGatewaysResolver.getGateway(innerGateways),
            customGateways = customGateways,
            replaceOriginalHost = true // For internal calls original IPFS host should be replaced in order to avoid rate limit of the original gateway
        )
    }

    /**
     * Used to build url exposed to the DB cache or API responses
     * @param publicGateways for override public gateways list
     * @param customGateways for override custom gateways list
     */
    fun resolvePublicHttpUrl(
        url: String,
        publicGateways: List<String> = emptyList(),
        customGateways: List<String> = emptyList()
    ): String =
        resolveHttpUrl(
            url = url,
            gateway = publicGatewayResolver.getGateway(publicGateways),
            customGateways = customGateways,
            replaceOriginalHost = false // For public IPFS urls we want to keep original gateway URL (if possible)
        )

    private fun resolveHttpUrl(
        url: String,
        gateway: String,
        customGateways: List<String> = emptyList(),
        replaceOriginalHost: Boolean
    ): String {

        embeddedImageChecker.check(url)?.let { return it }

        // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
        foreignerIpfsUriChecker.check(
            url = url,
            gateway = gateway,
            replaceOriginalHost = replaceOriginalHost,
            customGateways = customGateways
        )?.let { return it.encodeHtmlUrl() }

        // Checking prefixed IPFS URI like ipfs://Qmlalala
        abstractIpfsUrlChecker.check(
            url = url,
            gateway = gateway
        )?.let { return it.encodeHtmlUrl() }

        return when {
            url.startsWith("http") -> url
            url.startsWith("Qm") -> "$gateway/ipfs/$url"
            else -> "$gateway/${url.removeLeadingSlashes()}"
        }.encodeHtmlUrl()
    }

}
