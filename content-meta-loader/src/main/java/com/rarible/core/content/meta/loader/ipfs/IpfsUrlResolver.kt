package com.rarible.core.content.meta.loader.ipfs

import org.springframework.stereotype.Component
import java.net.URL
import java.util.Random
import java.util.regex.Pattern

@Component
class IpfsUrlResolver(
    ipfsPublicGateway: String,
    ipfsInnerGateways: String,
    ipfsCustomGateways: String? = null
) {

    val predefinedPublicGateway = ipfsPublicGateway.trimEnd('/')
    val predefinedInnerGateways = ipfsInnerGateways.split(",").map { it.trimEnd('/') }
    val predefinedCustomGateways = ipfsCustomGateways?.split(",")?.map { it.trimEnd('/') } ?: emptyList()

    fun isCid(test: String): Boolean {
        return CID_PATTERN.matcher(test).matches()
    }

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     * @param innerGateways for override inner gateways list
     * @param customGateways for override custom gateways list
     */
    fun resolveInnerHttpUrl(
        uri: String,
        innerGateways: List<String> = emptyList(),
        customGateways: List<String> = emptyList()
    ): String {
        return resolveHttpUrl(
            uri = uri,
            gateway = getRandomGateway(innerGateways.ifEmpty { predefinedInnerGateways }),
            customGateways = customGateways,
            replaceOriginalHost = true // For internal calls original IPFS host should be replaced in order to avoid rate limit of the original gateway
        )
    }

    /**
     * Used to build url exposed to the DB cache or API responses
     * @param publicGateway for override public gateway
     * @param customGateways for override custom gateways list
     */
    fun resolvePublicHttpUrl(
        uri: String,
        publicGateway: String? = null,
        customGateways: List<String> = emptyList()
    ): String =
        resolveHttpUrl(
            uri = uri,
            gateway = publicGateway ?: predefinedPublicGateway,
            customGateways = customGateways,
            replaceOriginalHost = false // For public IPFS urls we want to keep original gateway URL (if possible)
        )

    private fun resolveHttpUrl(
        uri: String,
        gateway: String,
        customGateways: List<String> = emptyList(),
        replaceOriginalHost: Boolean
    ): String {
        // Embedded image, return 'as is'
        if (uri.startsWith(SVG_START)) {
            return uri
        }

        // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
        checkForeignIpfsUri(
            uri = uri,
            gateway = gateway,
            replaceOriginalHost = replaceOriginalHost,
            customGateways = customGateways
        )?.let { return it.encodeHtmlUrl() }

        // Checking prefixed IPFS URI like ipfs://Qmlalala
        checkIpfsAbstractUrl(uri, gateway)?.let { return it.encodeHtmlUrl() }

        return when {
            uri.startsWith("http") -> uri
            uri.startsWith("Qm") -> "$gateway/ipfs/$uri"
            else -> "$gateway/${uri.removeLeadingSlashes()}"
        }.encodeHtmlUrl()
    }

    private fun checkForeignIpfsUri(
        uri: String,
        gateway: String,
        replaceOriginalHost: Boolean,
        customGateways: List<String> = emptyList()
    ): String? {
        val ipfsPathIndex = uri.lastIndexOf(IPFS_PATH_PART)
        if (ipfsPathIndex < 0) {
            return null
        }

        // If there is IPFS URL with one of legacy gateways, we need to replace it with actual public gateway
        for (legacy in customGateways.ifEmpty { predefinedCustomGateways }) {
            if (uri.startsWith(legacy)) {
                return gateway + uri.substring(legacy.length)
            }
        }

        // If URL is valid, and we want to keep original IPFS gateway, return 'as is'
        if (!replaceOriginalHost && isValidUrl(uri)) {
            return uri
        }

        val pathEnd = uri.substring(ipfsPathIndex + IPFS_PATH_PART.length).removeLeadingSlashes()
        // Works only for IPFS CIDs
        if (!isCid(pathEnd.substringBefore("/"))) {
            return null
        }

        return "$gateway/ipfs/$pathEnd"
    }

    companion object {

        private val CID_PATTERN = Pattern.compile(
            "Qm[1-9A-HJ-NP-Za-km-z]{44,}|b[A-Za-z2-7]{58,}|B[A-Z2-7]{58,}|z[1-9A-HJ-NP-Za-km-z]{48,}|F[0-9A-F]{50,}|f[0-9a-f]{50,}"
        )

        private const val IPFS_PREFIX = "ipfs:/"
        private const val IPFS_PATH_PART = "/ipfs/"
        private const val SVG_START = "<svg"

        private val IPFS_PREFIXES = listOf(
            "ipfs:///ipfs/",
            "ipfs://ipfs/",
            "ipfs:/ipfs/",
            IPFS_PREFIX
        )

        private fun getRandomGateway(gateways: List<String>): String = gateways[Random().nextInt(gateways.size)]

        private fun checkIpfsAbstractUrl(ipfsUri: String, gateway: String): String? {
            if (ipfsUri.length < IPFS_PREFIX.length) {
                return null
            }

            // Here we're checking links started with 'ipfs:'
            // In some cases there could be prefix in upper/mixed case like 'Ipfs'
            val potentialIpfsPrefix = ipfsUri.substring(0, IPFS_PREFIX.length).lowercase()

            // IPFS prefix not found, abort
            if (potentialIpfsPrefix != IPFS_PREFIX) {
                return null
            }

            val lowerCaseIpfsPrefixUri = IPFS_PREFIX + ipfsUri.substring(IPFS_PREFIX.length).removeLeadingSlashes()

            for (prefix in IPFS_PREFIXES) {
                if (lowerCaseIpfsPrefixUri.startsWith(prefix)) {
                    val path = lowerCaseIpfsPrefixUri.substring(prefix.length)
                    return "$gateway/ipfs/$path"
                }
            }
            // Should not happen, we already found IPFS prefix
            return null
        }

        private fun isValidUrl(uri: String): Boolean {
            return try {
                val url = URL(uri)
                (url.protocol == "http" || url.protocol == "https")
            } catch (e: Exception) {
                false
            }
        }

        private fun String.encodeHtmlUrl(): String {
            return this.replace(" ", "%20")
        }

        private fun String.removeLeadingSlashes(): String {
            var result = this
            while (result.startsWith('/')) {
                result = result.trimStart('/')
            }
            return result
        }
    }
}
