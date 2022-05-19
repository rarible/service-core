package com.rarible.core.content.meta.loader.ipfs

import com.rarible.core.content.meta.loader.ipfs.checker.AbstractIpfsUrlChecker
import com.rarible.core.content.meta.loader.ipfs.checker.ForeignIpfsUriChecker

class IpfsUrlParser(
    val foreignIpfsUriChecker: ForeignIpfsUriChecker,
    val abstractIpfsUrlChecker: AbstractIpfsUrlChecker
) {

    fun parse(url: String): MetaUrl {
        // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
        foreignIpfsUriChecker.check(url)
            ?.let { return it }

        // Checking prefixed IPFS URI like ipfs://Qmlalala
        abstractIpfsUrlChecker.check(url)
            ?.let { return it }

        return parseOtherCases(url)
    }

    private fun parseOtherCases(url: String): MetaUrl {
        return when {
            url.startsWith(SimpleHttpUrl.PREFIX) -> SimpleHttpUrl(url)
            url.startsWith("Qm") -> {
                IpfsUrl(
                    source = url,
                    originalGateway = null,
                    ipfsPath = "/ipfs/$url"
                )
            }
            else -> {
                IpfsUrl(
                    source = url,
                    originalGateway = null,
                    ipfsPath = url.removeLeadingSlashes()
                )
            }
        }
    }

}
