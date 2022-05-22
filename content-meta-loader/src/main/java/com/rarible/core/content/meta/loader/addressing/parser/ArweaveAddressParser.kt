package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.ArweaveUrl

class ArweaveAddressParser : AddressParser {

    override fun parse(url: String): ArweaveUrl? =
        when {
            url.startsWith(ArweaveUrl.AR_PREFIX) -> {
                ArweaveUrl(
                    origin = url,
                    originalGateway = null,
                    path = url.substring(ArweaveUrl.AR_PREFIX.length)  // TODO Write a test
                )
            }
            url.startsWith(ArweaveUrl.ARWEAVE_GATEWAY) -> {
                ArweaveUrl(
                    origin = url,
                    originalGateway = ArweaveUrl.ARWEAVE_GATEWAY,
                    path = url.substring(ArweaveUrl.ARWEAVE_GATEWAY.length)
                )
            }
            else -> null
        }
}
