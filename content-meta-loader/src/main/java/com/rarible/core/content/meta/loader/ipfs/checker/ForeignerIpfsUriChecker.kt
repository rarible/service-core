package com.rarible.core.content.meta.loader.ipfs.checker

import com.rarible.core.content.meta.loader.ipfs.CidResolver
import com.rarible.core.content.meta.loader.ipfs.GatewayResolver
import com.rarible.core.content.meta.loader.ipfs.isValidUrl
import com.rarible.core.content.meta.loader.ipfs.removeLeadingSlashes
import org.springframework.stereotype.Component

@Component
class ForeignerIpfsUriChecker(
    val customGatewaysResolver: GatewayResolver,
    val cidResolver: CidResolver,
) {

    // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
    fun check(
        url: String,
        gateway: String,
        replaceOriginalHost: Boolean,
        customGateways: List<String> = emptyList()
    ): String? {
        val ipfsPathIndex = url.lastIndexOf(IPFS_PATH_PART)
        if (ipfsPathIndex < 0) {
            return null
        }

        // If there is IPFS URL with one of legacy gateways, we need to replace it with actual public gateway
        for (legacy in customGatewaysResolver.getGateways(customGateways)) {
            if (url.startsWith(legacy)) {                           // TODO Move to another case?
                return gateway + url.substring(legacy.length)
            }
        }

        // If URL is valid, and we want to keep original IPFS gateway, return 'as is'
        if (!replaceOriginalHost && url.isValidUrl()) {
            return url
        }

        val pathEnd = url.substring(ipfsPathIndex + IPFS_PATH_PART.length).removeLeadingSlashes()
        // Works only for IPFS CIDs
        if (!cidResolver.isCid(pathEnd.substringBefore("/"))) {
            return null
        }

        return "$gateway/ipfs/$pathEnd"
    }

    companion object {
        const val IPFS_PATH_PART = "/ipfs/"
    }
}
