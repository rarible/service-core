package com.rarible.core.content.meta.loader.ipfs


interface MetaUrl {
    fun getSourceUrl(): String
}

data class IpfsUrl(
    val source: String,
    val originalGateway: String?,
    val ipfsPath: String
) : MetaUrl {

    override fun getSourceUrl(): String = source

    fun resolveWithOriginalGateway(gateway: String): String = "$originalGateway${ipfsPath}" // TODO Use this instead MetaUrl.source ?

    fun resolveWithGateway(gateway: String): String = "${gateway}${ipfsPath}"
}

data class SimpleHttpUrl(
    val source: String
) : MetaUrl {

    override fun getSourceUrl(): String = source

    companion object {
        const val PREFIX = "http"
    }
}
