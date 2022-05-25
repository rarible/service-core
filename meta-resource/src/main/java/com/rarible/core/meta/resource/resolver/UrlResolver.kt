package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.Cid
import com.rarible.core.meta.resource.HttpUrl
import com.rarible.core.meta.resource.IpfsUrl
import com.rarible.core.meta.resource.UrlResource

class UrlResolver(
    private val ipfsGatewayResolver: IpfsGatewayResolver,
    private val ipfsCidGatewayResolver: IpfsCidGatewayResolver,
    private val arweaveGatewayResolver: ArweaveGatewayResolver,
    private val simpleHttpGatewayResolver: SimpleHttpGatewayResolver
) {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    fun resolveInnerLink(url: UrlResource): String =
        resolveInternal(url = url, isPublic = false)

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    fun resolvePublicLink(resource: UrlResource): String =
        resolveInternal(url = resource, isPublic = true)

    private fun resolveInternal(
        url: UrlResource,
        isPublic: Boolean
    ): String =
        when (url) {
            is HttpUrl -> simpleHttpGatewayResolver.resolveLink(url, isPublic)
            is IpfsUrl -> ipfsGatewayResolver.resolveLink(url, isPublic)
            is Cid -> ipfsCidGatewayResolver.resolveLink(url, isPublic)
            is ArweaveUrl -> arweaveGatewayResolver.resolveLink(url, isPublic)
            else -> throw UnsupportedOperationException("Unsupported resolving for ${url.javaClass.name}")
        }
}
