package com.rarible.core.content.meta.loader.ipfs.checker

import com.rarible.core.content.meta.loader.ipfs.IpfsCidResolver
import com.rarible.core.content.meta.loader.ipfs.GatewayProvider
import com.rarible.core.content.meta.loader.ipfs.IpfsUrl
import com.rarible.core.content.meta.loader.ipfs.isValidUrl
import com.rarible.core.content.meta.loader.ipfs.removeLeadingSlashes
import org.springframework.stereotype.Component

@Component
class ForeignIpfsUriChecker(
    val ipfsCidResolver: IpfsCidResolver
) {

    // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala
    fun check(url: String): IpfsUrl? {
        val ipfsPathIndex = getIpfsPathIndex(url) ?: return null
        // TODO Should check url.isValidUrl() ?

        val pathEnd = url.substring(ipfsPathIndex + IPFS_PATH_PART.length).removeLeadingSlashes()
        // Works only for IPFS CIDs
        if (!ipfsCidResolver.isCid(pathEnd.substringBefore("/"))) {  // TODO Maybe add check into AbstractIpfsUrlChecker ?
            return null
        }

//        return "$gateway/ipfs/$pathEnd"
        return IpfsUrl(
            source = url,
            originalGateway = null, // TODO How to find?
            ipfsPath = "/ipfs/$pathEnd"
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
