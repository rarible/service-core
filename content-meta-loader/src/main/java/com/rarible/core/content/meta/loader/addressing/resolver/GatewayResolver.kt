package com.rarible.core.content.meta.loader.addressing.resolver

import com.rarible.core.content.meta.loader.addressing.ArweaveUrl
import com.rarible.core.content.meta.loader.addressing.RawCidAddress
import com.rarible.core.content.meta.loader.addressing.SLASH
import com.rarible.core.content.meta.loader.addressing.SimpleHttpUrl
import com.rarible.core.content.meta.loader.addressing.GatewayProvider
import com.rarible.core.content.meta.loader.addressing.IpfsUrl.Companion.IPFS

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

class SimpleHttpGatewayResolver : GatewayResolver<SimpleHttpUrl> {

    override fun resolveInnerAddress(address: SimpleHttpUrl): String = address.origin

    override fun resolvePublicAddress(address: SimpleHttpUrl): String = address.origin
}

class RawCidGatewayResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val innerGatewaysProvider: GatewayProvider
) : GatewayResolver<RawCidAddress> {

    override fun resolveInnerAddress(address: RawCidAddress): String =
        resolveWithGateway(address, innerGatewaysProvider.getGateway())

    override fun resolvePublicAddress(address: RawCidAddress): String =
        resolveWithGateway(address, publicGatewayProvider.getGateway())

    private fun resolveWithGateway(address: RawCidAddress, gateway: String): String =  // TODO Add test
        if (address.additionalPath != null) {
            "$gateway$SLASH$IPFS$SLASH${address.cid}$SLASH${address.additionalPath}"
        } else {
            "$gateway$SLASH$IPFS$SLASH${address.cid}"
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
