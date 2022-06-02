package com.rarible.core.meta.resource

abstract class UrlResource {

    abstract val original: String
}

data class HttpUrl(
    override val original: String
) : UrlResource()

data class SchemaUrl(
    override val original: String,
    val gateway: String,
    val schema: String,
    val path: String
) : UrlResource()

data class IpfsUrl(
    override val original: String,
    val originalGateway: String?,
    val path: String
) : UrlResource() {

    companion object {

        const val IPFS = "ipfs"
        const val IPFS_PREFIX = "$IPFS:/"
        const val IPFS_PATH_PART = "/$IPFS/"
    }
}
