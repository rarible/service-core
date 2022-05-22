package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.ResourceAddress

interface AddressParser {
    fun parse(url: String): ResourceAddress?
}
