package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.UrlResource
import com.rarible.core.meta.resource.parser.ipfs.AbstractIpfsUrlResourceParser
import com.rarible.core.meta.resource.parser.ipfs.ForeignIpfsUrlResourceParser

class UrlResourceProcessor(
    private val urlResourceParserProvider: UrlResourceParserProvider
) {

    fun parse(link: String): UrlResource? {
        for (parser in urlResourceParserProvider.urlResourceParsers) {
            val urlResource = parser.parse(link.trim())
            if (urlResource != null) {
                return urlResource
            }
        }
        return null
    }
}

interface UrlResourceParserProvider {
    val urlResourceParsers: List<UrlResourceParser<UrlResource>>
}

class DefaultUrlResourceParserProvider(
    abstractIpfsUrlResourceParser: AbstractIpfsUrlResourceParser,
    foreignIpfsUrlResourceParser: ForeignIpfsUrlResourceParser,
    arweaveUrlParser: ArweaveUrlResourceParser,
    httpUrlParser: HttpUrlResourceParser,
    cidUrlResourceParser: CidUrlResourceParser
) : UrlResourceParserProvider {

    override val urlResourceParsers: List<UrlResourceParser<UrlResource>> =
        listOf(
            abstractIpfsUrlResourceParser,
            foreignIpfsUrlResourceParser,
            arweaveUrlParser,
            httpUrlParser,
            cidUrlResourceParser
        )
}
