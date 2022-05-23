package com.rarible.core.meta.resource

abstract class UrlResource {

    abstract val original: String
}

data class HttpUrl(
    override val original: String
) : UrlResource()

data class Cid(
    override val original: String,
    val cid: String,
    val additionalPath: String?
) : UrlResource()

abstract class GatewayUrl : UrlResource() {

    abstract val originalGateway: String?
    abstract val path: String
}

data class IpfsUrl(
    override val original: String,
    override val originalGateway: String?,
    override val path: String
) : GatewayUrl() {

    companion object {

        const val IPFS = "ipfs"
        const val IPFS_PREFIX = "$IPFS:/"
        const val IPFS_PATH_PART = "/$IPFS/"
    }
}

data class ArweaveUrl(
    override val original: String,
    override val originalGateway: String?,
    override val path: String
) : GatewayUrl() {

    companion object {

        const val AR_PREFIX = "ar://"
        const val ARWEAVE_GATEWAY = "https://arweave.net"
    }
}
