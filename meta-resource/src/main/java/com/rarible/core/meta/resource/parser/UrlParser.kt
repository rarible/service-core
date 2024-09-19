package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.model.UrlResource
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
        val sanitized = sanitize(url)
        return resourceParsers.firstNotNullOfOrNull { it.parse(sanitized) }
    }

    private fun sanitize(url: String): String {
        var result = url.trim().trimStart('/')
        while (result.length > 1 && result.first() == result.last() && QUOTES.contains(result.first())) {
            result = result.substring(1, result.length - 1)
        }
        return result
    }

    companion object {
        private val QUOTES = setOf('"', '\'')
    }
}
