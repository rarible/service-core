package com.rarible.core.content.meta.loader.addressing

abstract class ResourceAddress {
    abstract val origin: String
}

data class SimpleHttpUrl(
    override val origin: String
) : ResourceAddress()

data class RawCidAddress(
    override val origin: String,
    val cid: String,
    val additionalPath: String?
) : ResourceAddress()

abstract class SplitUrl : ResourceAddress() {
    abstract val originalGateway: String?
    abstract val path: String
}

data class IpfsUrl(
    override val origin: String,
    override val originalGateway: String?,
    override val path: String
) : SplitUrl() {

    companion object {
        const val IPFS = "ipfs"
        const val IPFS_PREFIX = "$IPFS:$SLASH"
        const val IPFS_PATH_PART = "$SLASH$IPFS$SLASH"
    }
}

data class ArweaveUrl(
    override val origin: String,
    override val originalGateway: String?,
    override val path: String
) : SplitUrl() {

    companion object {
        const val AR_PREFIX = "ar://"
        const val ARWEAVE_GATEWAY = "https://arweave.net"
    }
}
