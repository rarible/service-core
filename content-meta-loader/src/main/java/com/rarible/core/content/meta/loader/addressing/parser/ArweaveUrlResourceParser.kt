package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.ArweaveUrl

class ArweaveUrlResourceParser : UrlResourceParser<ArweaveUrl> {

    override fun parse(url: String): ArweaveUrl? =
        when {
            url.startsWith(ArweaveUrl.AR_PREFIX) -> {
                ArweaveUrl(
                    original = url,
                    originalGateway = null,
                    path = "/${url.substring(ArweaveUrl.AR_PREFIX.length)}"
                )
            }
            url.startsWith(ArweaveUrl.ARWEAVE_GATEWAY) -> {
                ArweaveUrl(
                    original = url,
                    originalGateway = ArweaveUrl.ARWEAVE_GATEWAY,
                    path = url.substring(ArweaveUrl.ARWEAVE_GATEWAY.length)
                )
            }
            else -> null
        }
}
