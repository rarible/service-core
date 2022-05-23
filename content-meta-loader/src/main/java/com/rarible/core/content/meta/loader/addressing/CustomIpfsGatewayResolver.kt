package com.rarible.core.content.meta.loader.addressing

interface CustomIpfsGatewayResolver {

    fun getUrlResource(ipfsUrl: IpfsUrl, gateway: String, replaceOriginalHost: Boolean): String?
}