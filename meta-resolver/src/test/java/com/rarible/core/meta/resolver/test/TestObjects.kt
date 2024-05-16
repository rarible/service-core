package com.rarible.core.meta.resolver.test

import com.rarible.core.meta.resolver.url.UrlService
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.ConstantGatewayProvider
import com.rarible.core.meta.resource.resolver.IpfsGatewayResolver
import com.rarible.core.meta.resource.resolver.UrlResolver

object TestObjects {

    private const val IPFS_PUBLIC_GATEWAY = "https://ipfs.io"

    val urlParser = UrlParser()

    private val urlResolver = UrlResolver(
        ipfsGatewayResolver = IpfsGatewayResolver(
            publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY.trimEnd('/')),
            internalGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY.trimEnd('/'))
        )
    )

    val urlService = UrlService(
        urlParser = urlParser,
        urlResolver = urlResolver,
    )
}
