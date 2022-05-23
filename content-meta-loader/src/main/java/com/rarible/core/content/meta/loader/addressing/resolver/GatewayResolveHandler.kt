package com.rarible.core.content.meta.loader.addressing.resolver

import com.rarible.core.content.meta.loader.addressing.ArweaveUrl
import com.rarible.core.content.meta.loader.addressing.IpfsUrl
import com.rarible.core.content.meta.loader.addressing.RawCidAddress
import com.rarible.core.content.meta.loader.addressing.ResourceAddress
import com.rarible.core.content.meta.loader.addressing.SimpleHttpUrl

class GatewayResolveHandler(
    private val ipfsGatewayResolver: IpfsGatewayResolver,
    private val rawCidGatewayResolver: RawCidGatewayResolver,
    private val arweaveGatewayResolver: ArweaveGatewayResolver,
    private val simpleHttpGatewayResolver: SimpleHttpGatewayResolver
) {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    fun resolveInnerAddress(address: ResourceAddress): String =
        resolveInternal(url = address, isPublic = false)

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    fun resolvePublicAddress(address: ResourceAddress): String =
        resolveInternal(url = address, isPublic = true)

    private fun resolveInternal(
        url: ResourceAddress,
        isPublic: Boolean
    ): String =
        when (url) {
            is SimpleHttpUrl -> if (isPublic) simpleHttpGatewayResolver.resolvePublicAddress(url) else simpleHttpGatewayResolver.resolveInnerAddress(url)
            is IpfsUrl -> if (isPublic) ipfsGatewayResolver.resolvePublicAddress(url) else ipfsGatewayResolver.resolveInnerAddress(url)
            is RawCidAddress -> if (isPublic) rawCidGatewayResolver.resolvePublicAddress(url) else rawCidGatewayResolver.resolveInnerAddress(url)
            is ArweaveUrl -> if (isPublic) arweaveGatewayResolver.resolvePublicAddress(url) else arweaveGatewayResolver.resolveInnerAddress(url)
            else -> throw UnsupportedOperationException("Unsupported resolving for ${url.javaClass.name}")
        }
}
