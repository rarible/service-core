package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.Cid
import com.rarible.core.meta.resource.GatewayProvider
import com.rarible.core.meta.resource.HttpUrl
import com.rarible.core.meta.resource.IpfsUrl.Companion.IPFS
import com.rarible.core.meta.resource.UrlResource

interface GatewayResolver<T : UrlResource> {

    /**
     * Private: Used only for internal operations, such urls should NOT be stored anywhere
     * Public:  Used to build url exposed to the DB cache or API responses
     */
    fun resolveLink(resource: T, isPublic: Boolean): String
}

class SimpleHttpGatewayResolver : GatewayResolver<HttpUrl> {

    override fun resolveLink(resource: HttpUrl, isPublic: Boolean): String = resource.original
}

class IpfsCidGatewayResolver(
    private val publicGatewayProvider: GatewayProvider,
    private val innerGatewaysProvider: GatewayProvider
) : GatewayResolver<Cid> {

    override fun resolveLink(resource: Cid, isPublic: Boolean): String {
        val gateway = if (isPublic) publicGatewayProvider.getGateway() else innerGatewaysProvider.getGateway()
        return if (resource.subPath != null) {
            "$gateway/$IPFS/${resource.cid}${resource.subPath}"
        } else {
            "$gateway/$IPFS/${resource.cid}"
        }
    }
}

class ArweaveGatewayResolver(
    private val arweaveGatewayProvider: GatewayProvider
) : GatewayResolver<ArweaveUrl> {

    override fun resolveLink(resource: ArweaveUrl, isPublic: Boolean): String {
        val gateway = resource.originalGateway ?: arweaveGatewayProvider.getGateway()
        return "${gateway}${resource.path}"
    }
}
