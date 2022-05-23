package com.rarible.core.content.meta.loader.addressing.parser.ipfs

import com.rarible.core.content.meta.loader.addressing.IpfsUrl
import com.rarible.core.content.meta.loader.addressing.IpfsUrl.Companion.IPFS_PREFIX
import com.rarible.core.content.meta.loader.addressing.parser.UrlResourceParser
import com.rarible.core.content.meta.loader.addressing.removeLeadingSlashes

class AbstractIpfsUrlResourceParser : UrlResourceParser<IpfsUrl> {

    // Checking prefixed IPFS URI like ipfs://Qmlalala
    override fun parse(url: String): IpfsUrl? {
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
                val path = lowerCaseIpfsPrefixUri.substring(prefix.length).removeLeadingSlashes()
                return IpfsUrl(
                    original = url,
                    originalGateway = null, // Because URI like ipfs://Qmlalala
                    path = "/$path"
                )
            }
        }
        // Should not happen, we already found IPFS prefix
        return null
    }

    companion object {
        private val IPFS_PREFIXES = listOf(
            "ipfs:///ipfs/",
            "ipfs://ipfs/",
            "ipfs:/ipfs/",
            IPFS_PREFIX
        )
    }
}
