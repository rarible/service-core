package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.Cid
import com.rarible.core.meta.resource.HttpUrl
import com.rarible.core.meta.resource.IpfsUrl
import com.rarible.core.meta.resource.UrlResource

class GatewayResolveHandler(
    private val ipfsGatewayResolver: IpfsGatewayResolver,
    private val rawCidGatewayResolver: RawCidGatewayResolver,
    private val arweaveGatewayResolver: ArweaveGatewayResolver,
    private val simpleHttpGatewayResolver: SimpleHttpGatewayResolver
) {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    fun resolveInnerAddress(url: UrlResource): String =
        resolveInternal(url = url, isPublic = false)

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    fun resolvePublicAddress(address: UrlResource): String =
        resolveInternal(url = address, isPublic = true)

    private fun resolveInternal(
        url: UrlResource,
        isPublic: Boolean
    ): String =
        when (url) {
            is HttpUrl -> if (isPublic) simpleHttpGatewayResolver.resolvePublicAddress(
                url
            ) else simpleHttpGatewayResolver.resolveInnerAddress(url)
            is IpfsUrl -> if (isPublic) ipfsGatewayResolver.resolvePublicAddress(
                url
            ) else ipfsGatewayResolver.resolveInnerAddress(url)
            is Cid -> if (isPublic) rawCidGatewayResolver.resolvePublicAddress(
                url
            ) else rawCidGatewayResolver.resolveInnerAddress(url)
            is ArweaveUrl -> if (isPublic) arweaveGatewayResolver.resolvePublicAddress(
                url
            ) else arweaveGatewayResolver.resolveInnerAddress(url)
            else -> throw UnsupportedOperationException("Unsupported resolving for ${url.javaClass.name}")
        }
}
