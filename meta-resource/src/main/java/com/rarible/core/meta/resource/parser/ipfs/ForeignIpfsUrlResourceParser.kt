package com.rarible.core.meta.resource.parser.ipfs

import com.rarible.core.meta.resource.IpfsUrl
import com.rarible.core.meta.resource.IpfsUrl.Companion.IPFS_PATH_PART
import com.rarible.core.meta.resource.cid.CidV1Validator
import com.rarible.core.meta.resource.cid.CidValidator
import com.rarible.core.meta.resource.parser.UrlResourceParser
import com.rarible.core.meta.resource.util.removeLeadingSlashes

class ForeignIpfsUrlResourceParser(
    private val cidValidator: CidValidator = CidV1Validator
) : UrlResourceParser<IpfsUrl> {

    // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
    override fun parse(url: String): IpfsUrl? {
        val ipfsPathIndex = getIpfsPathIndex(url) ?: return null

        val pathEnd = url.substring(ipfsPathIndex + IPFS_PATH_PART.length).removeLeadingSlashes()

        // Works for CID v1.0
        val cid = pathEnd.substringBefore('/')
        if (!cidValidator.isCid(cid)) {
            return null
        }

        return IpfsUrl(
            original = url,
            originalGateway = url.substring(0, ipfsPathIndex), // TODO Test it
            path = pathEnd
        )
    }

    private fun getIpfsPathIndex(url: String): Int? {
        val ipfsPathIndex = url.lastIndexOf(IPFS_PATH_PART)
        if (ipfsPathIndex < 0) {
            return null
        }
        return ipfsPathIndex
    }
}
