package com.rarible.core.content.meta.loader.addressing.parser.ipfs

import com.rarible.core.content.meta.loader.addressing.removeLeadingSlashes
import com.rarible.core.content.meta.loader.addressing.IpfsUrl
import com.rarible.core.content.meta.loader.addressing.cid.CidValidator
import org.springframework.stereotype.Component

@Component
class ForeignIpfsUriChecker(
    val cidOneValidator: CidValidator
) {

    // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
    fun check(url: String): IpfsUrl? {
        val ipfsPathIndex = getIpfsPathIndex(url) ?: return null
        // TODO Should check url.isValidUrl() ?

        val pathEnd = url.substring(ipfsPathIndex + IPFS_PATH_PART.length).removeLeadingSlashes()
        // Works only for IPFS CIDs
        if (!cidOneValidator.isCid(pathEnd.substringBefore("/"))) {  // TODO Maybe add check into AbstractIpfsUrlChecker ?
            return null
        }

//        return "$gateway/ipfs/$pathEnd"
        return IpfsUrl(
            origin = url,
            originalGateway = null, // TODO How to find?
            path = "/ipfs/$pathEnd"  // TODO Remove prefix
        )
    }

    fun getIpfsPathIndex(url: String): Int? {
        val ipfsPathIndex = url.lastIndexOf(IPFS_PATH_PART)
        if (ipfsPathIndex < 0) {
            return null
        }
        return ipfsPathIndex
    }

    companion object {
        const val IPFS_PATH_PART = "/ipfs/"
    }
}
