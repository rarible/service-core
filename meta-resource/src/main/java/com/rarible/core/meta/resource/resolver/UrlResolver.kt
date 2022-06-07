package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.model.SchemaUrl
import com.rarible.core.meta.resource.model.UrlResource

class UrlResolver(
    private val ipfsGatewayResolver: IpfsGatewayResolver
) {

    /**
     * Used only for internal operations, such urls should NOT be stored anywhere
     */
    fun resolveInternalUrl(resource: UrlResource): String =
        resolveInternal(resource = resource, isPublic = false)

    /**
     * Used to build url exposed to the DB cache or API responses
     */
    fun resolvePublicUrl(resource: UrlResource): String =
        resolveInternal(resource = resource, isPublic = true)

    private fun resolveInternal(
        resource: UrlResource,
        isPublic: Boolean
    ): String =
        when (resource) {
            is HttpUrl -> resource.original
            is SchemaUrl -> "${resource.gateway}/${resource.path}"
            is IpfsUrl -> ipfsGatewayResolver.resolveUrl(resource, isPublic)
            else -> throw UnsupportedOperationException("Unsupported resolving for ${resource.javaClass.name}")
        }
}
