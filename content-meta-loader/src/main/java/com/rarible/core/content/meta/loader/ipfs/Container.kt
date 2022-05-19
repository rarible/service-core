package com.rarible.core.content.meta.loader.ipfs

class Container(
    private val ipfsUrlParser: IpfsUrlParser,
    private val ipfsUrlResolver: IpfsUrlResolver
) {

    fun resolvePublicHttpUrl(url: String): String {
        val ipfsUrl = ipfsUrlParser.parse(url)
        return ipfsUrlResolver.resolvePublicHttpUrl(ipfsUrl)
    }

    fun resolveInnerHttpUrl(url: String): String {
        val ipfsUrl = ipfsUrlParser.parse(url)
        return ipfsUrlResolver.resolveInnerHttpUrl(ipfsUrl)
    }
}
