package com.rarible.core.meta.resource

interface CustomIpfsGatewayResolver {

    fun getUrlResource(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String?
}
