package com.rarible.core.content.meta.loader.ipfs.checker

import com.rarible.core.content.meta.loader.ipfs.removeLeadingSlashes
import org.springframework.stereotype.Component

@Component
class AbstractIpfsUrlChecker {

    // Checking prefixed IPFS URI like ipfs://Qmlalala
    fun check(url: String, gateway: String): String? {
        if (url.length < IPFS_PREFIX.length) {
            return null
        }

        // Here we're checking links started with 'ipfs:'
        // In some cases there could be prefix in upper/mixed case like 'Ipfs'
        val potentialIpfsPrefix = url.substring(0, IPFS_PREFIX.length).lowercase()

        // IPFS prefix not found, abort
        if (potentialIpfsPrefix != IPFS_PREFIX) {
            return null
        }

        val lowerCaseIpfsPrefixUri = IPFS_PREFIX + url.substring(IPFS_PREFIX.length).removeLeadingSlashes()

        for (prefix in IPFS_PREFIXES) {
            if (lowerCaseIpfsPrefixUri.startsWith(prefix)) {
                val path = lowerCaseIpfsPrefixUri.substring(prefix.length)
                return "$gateway/ipfs/$path"
            }
        }
        // Should not happen, we already found IPFS prefix
        return null
    }

    companion object {
        private const val IPFS_PREFIX = "ipfs:/"
        private val IPFS_PREFIXES = listOf(
            "ipfs:///ipfs/",
            "ipfs://ipfs/",
            "ipfs:/ipfs/",
            IPFS_PREFIX
        )
    }
}
