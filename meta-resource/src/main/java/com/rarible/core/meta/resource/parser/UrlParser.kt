package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.UrlResource
import com.rarible.core.meta.resource.parser.ipfs.IpfsUrlResourceParser

class UrlParser(
    private val resourceParsers: List<UrlResourceParser<UrlResource>> =
        listOf(
            IpfsUrlResourceParser(),
            SchemaUrlResourceParser(),
            HttpUrlResourceParser()
        )
) {

    fun parse(url: String): UrlResource? {
        val sanitized = url.trim()
        return resourceParsers.firstNotNullOfOrNull { it.parse(sanitized) }
    }
}
