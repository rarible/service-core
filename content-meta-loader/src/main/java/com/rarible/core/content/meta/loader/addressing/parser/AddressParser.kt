package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.ResourceAddress

interface AddressParser<out T : ResourceAddress> {
    fun parse(url: String): T?
}
