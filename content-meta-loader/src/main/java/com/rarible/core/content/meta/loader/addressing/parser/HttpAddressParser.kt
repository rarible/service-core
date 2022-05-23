package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.SimpleHttpUrl
import com.rarible.core.content.meta.loader.addressing.isValidUrl

class HttpAddressParser : AddressParser<SimpleHttpUrl> {

    override fun parse(address: String): SimpleHttpUrl? {
        if (address.isValidUrl()) {
            return SimpleHttpUrl(address)
        }
        return null
    }
}
