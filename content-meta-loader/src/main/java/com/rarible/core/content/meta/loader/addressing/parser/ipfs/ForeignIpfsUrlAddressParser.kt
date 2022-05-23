package com.rarible.core.content.meta.loader.addressing.parser.ipfs

import com.rarible.core.content.meta.loader.addressing.IpfsUrl
import com.rarible.core.content.meta.loader.addressing.IpfsUrl.Companion.IPFS_PATH_PART
import com.rarible.core.content.meta.loader.addressing.SLASH
import com.rarible.core.content.meta.loader.addressing.cid.CidValidator
import com.rarible.core.content.meta.loader.addressing.parser.AddressParser
import com.rarible.core.content.meta.loader.addressing.removeLeadingSlashes

class ForeignIpfsUrlAddressParser(
    val cidOneValidator: CidValidator
) : AddressParser<IpfsUrl> {

    // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
    override fun parse(url: String): IpfsUrl? {
        val ipfsPathIndex = getIpfsPathIndex(url) ?: return null
        // TODO Should check url.isValidUrl() ?

        val pathEnd = url.substring(ipfsPathIndex + IPFS_PATH_PART.length).removeLeadingSlashes()
        // Works only for IPFS CIDs
        if (!cidOneValidator.isCid(pathEnd.substringBefore("/"))) {  // TODO Maybe add check into AbstractIpfsUrlChecker ?
            return null
        }

        return IpfsUrl(
            origin = url,
            originalGateway = url.substring(0, ipfsPathIndex), // TODO Test it
            path = "$SLASH$pathEnd"
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
