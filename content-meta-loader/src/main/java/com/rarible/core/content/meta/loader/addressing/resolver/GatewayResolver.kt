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
    fun resolveInnerAddress(url: T): String

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    fun resolvePublicAddress(url: T): String
}

class SimpleHttpGatewayResolver : GatewayResolver<SimpleHttpUrl> {

    override fun resolveInnerAddress(url: SimpleHttpUrl): String = url.origin

    override fun resolvePublicAddress(url: SimpleHttpUrl): String = url.origin
}

class RawCidGatewayResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val innerGatewaysProvider: GatewayProvider
) : GatewayResolver<RawCidAddress> {

    override fun resolveInnerAddress(url: RawCidAddress): String =
        resolveWithGateway(url, innerGatewaysProvider.getGateway())

    override fun resolvePublicAddress(url: RawCidAddress): String =
        resolveWithGateway(url, publicGatewayProvider.getGateway())

    private fun resolveWithGateway(address: RawCidAddress, gateway: String): String =  // TODO Add test
        if (address.additionalPath != null) {
            "$gateway$SLASH$IPFS$SLASH${address.cid}$SLASH${address.additionalPath}"
        } else {
            "$gateway$SLASH$IPFS$SLASH${address.cid}"
        }
}

class ArweaveGatewayResolver : GatewayResolver<ArweaveUrl> {

    override fun resolveInnerAddress(url: ArweaveUrl): String = "${url.originalGateway}${url.path}"

    override fun resolvePublicAddress(url: ArweaveUrl): String = "${url.originalGateway}${url.path}"
}
