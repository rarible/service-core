package com.rarible.core.content.meta.loader.addressing

abstract class ResourceAddress {
    abstract val origin: String
}

data class SimpleHttpUrl(
    override val origin: String
) : ResourceAddress() {

    companion object {
        const val PREFIX = "http"
    }
}

data class RawCidAddress(
    override val origin: String,
    val cid: String,
    val additionalPath: String?
) : ResourceAddress() {

    fun resolveWithGateway(gateway: String): String =  // TODO Maybe move to resolvers?
        if (additionalPath != null) {
            "$gateway${SLASH}ipfs$SLASH$cid$SLASH$additionalPath"
        } else {
            "$gateway${SLASH}ipfs$SLASH$cid"
        }
}

abstract class SplitUrl : ResourceAddress() {
    abstract val originalGateway: String?
    abstract val path: String

    fun resolveWithOriginalGateway(): String = "$originalGateway${path}"

    fun resolveWithGateway(gateway: String): String = "${gateway}${path}"
}

data class IpfsUrl(
    override val origin: String,
    override val originalGateway: String?,
    override val path: String  // TODO Maybe add RawCidAddress here?
) : SplitUrl() {

    // TODO переопределить и добавить путь айпифиэс

}

data class ArweaveUrl(
    override val origin: String,
    override val originalGateway: String?,
    override val path: String
) : SplitUrl() {

    companion object {
        const val AR_PREFIX = "ar://"
        const val ARWEAVE_GATEWAY = "https://arweave.net/" // TODO Может сделать ресолвер для гейтвеев
    }
}
