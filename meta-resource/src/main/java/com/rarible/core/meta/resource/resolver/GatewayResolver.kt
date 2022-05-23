package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.Cid
import com.rarible.core.meta.resource.GatewayProvider
import com.rarible.core.meta.resource.HttpUrl
import com.rarible.core.meta.resource.IpfsUrl.Companion.IPFS

interface GatewayResolver<T> {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    fun resolveInnerAddress(address: T): String

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    fun resolvePublicAddress(address: T): String
}

class SimpleHttpGatewayResolver : GatewayResolver<HttpUrl> {

    override fun resolveInnerAddress(address: HttpUrl): String = address.original

    override fun resolvePublicAddress(address: HttpUrl): String = address.original
}

class RawCidGatewayResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val innerGatewaysProvider: GatewayProvider
) : GatewayResolver<Cid> {

    override fun resolveInnerAddress(address: Cid): String =
        resolveWithGateway(address, innerGatewaysProvider.getGateway())

    override fun resolvePublicAddress(address: Cid): String =
        resolveWithGateway(address, publicGatewayProvider.getGateway())

    private fun resolveWithGateway(address: Cid, gateway: String): String =  // TODO Add test
        if (address.additionalPath != null) {
            "$gateway/$IPFS/${address.cid}${address.additionalPath}"
        } else {
            "$gateway/$IPFS/${address.cid}"
        }
}

class ArweaveGatewayResolver(
    private val arweaveGatewayProvider: GatewayProvider
) : GatewayResolver<ArweaveUrl> {

    override fun resolveInnerAddress(address: ArweaveUrl): String = resolve(address)

    override fun resolvePublicAddress(address: ArweaveUrl): String = resolve(address)

    private fun resolve(address: ArweaveUrl): String {
        val gateway = address.originalGateway ?: arweaveGatewayProvider.getGateway()
        return "${gateway}${address.path}"
    }
}
