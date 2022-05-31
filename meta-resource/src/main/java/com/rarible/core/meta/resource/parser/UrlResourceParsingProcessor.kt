package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.UrlResource
import com.rarible.core.meta.resource.parser.ipfs.AbstractIpfsUrlResourceParser
import com.rarible.core.meta.resource.parser.ipfs.ForeignIpfsUrlResourceParser

class UrlResourceParsingProcessor(
    private val provider: UrlResourceParserProvider
) {

    fun parse(link: String): UrlResource? {
        for (parser in provider.parsers) {
            val urlResource = parser.parse(link.trim())
            if (urlResource != null) {
                return urlResource
            }
        }
        return null
    }
}

interface UrlResourceParserProvider {
    val parsers: List<UrlResourceParser<UrlResource>>
}

class DefaultUrlResourceParserProvider(
    abstractIpfsUrlResourceParser: AbstractIpfsUrlResourceParser,
    foreignIpfsUrlResourceParser: ForeignIpfsUrlResourceParser,
    arweaveUrlParser: ArweaveUrlResourceParser,
    httpUrlParser: HttpUrlResourceParser,
    cidUrlResourceParser: CidUrlResourceParser
) : UrlResourceParserProvider {

    override val parsers: List<UrlResourceParser<UrlResource>> =
        listOf(
            abstractIpfsUrlResourceParser,
            foreignIpfsUrlResourceParser,
            arweaveUrlParser,
            httpUrlParser,
            cidUrlResourceParser
        )
}
