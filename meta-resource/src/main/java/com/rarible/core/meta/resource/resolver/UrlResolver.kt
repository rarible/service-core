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
    fun resolveInnerLink(resource: UrlResource): String =
        resolveInternal(resource = resource, isPublic = false)

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    fun resolvePublicLink(resource: UrlResource): String =
        resolveInternal(resource = resource, isPublic = true)

    private fun resolveInternal(
        resource: UrlResource,
        isPublic: Boolean
    ): String =
        when (resource) {
            is HttpUrl -> simpleHttpGatewayResolver.resolveLink(resource, isPublic)
            is IpfsUrl -> ipfsGatewayResolver.resolveLink(resource, isPublic)
            is Cid -> ipfsCidGatewayResolver.resolveLink(resource, isPublic)
            is ArweaveUrl -> arweaveGatewayResolver.resolveLink(resource, isPublic)
            else -> throw UnsupportedOperationException("Unsupported resolving for ${resource.javaClass.name}")
        }
}
