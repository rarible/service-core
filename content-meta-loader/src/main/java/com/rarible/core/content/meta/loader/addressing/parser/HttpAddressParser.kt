package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.ResourceAddress
import com.rarible.core.content.meta.loader.addressing.SimpleHttpUrl

class HttpAddressParser: AddressParser {

    override fun parse(address: String): ResourceAddress? {
        if (address.startsWith(SimpleHttpUrl.PREFIX)) { // TODO Change to isValidUrl()
            return SimpleHttpUrl(address)
        }
        return null
    }
}
