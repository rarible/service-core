package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.UrlResource
import com.rarible.core.content.meta.loader.addressing.parser.ipfs.AbstractIpfsUrlResourceParser
import com.rarible.core.content.meta.loader.addressing.parser.ipfs.ForeignIpfsUrlResourceParser

class UrlResourceProcessor(
    private val addressParserProvider: UrlResourceParserProvider
) {

    fun parse(address: String): UrlResource? {
        for (parser in addressParserProvider.addressParsers) {
            val resourceAddress = parser.parse(address.trim())
            if (resourceAddress != null) {
                return resourceAddress
            }
        }
        return null
    }
}

class UrlResourceParserProvider(
    arweaveUrlParser: ArweaveUrlResourceParser,
    abstractIpfsAddressParser: AbstractIpfsUrlResourceParser,
    foreignIpfsUrlAddressParser: ForeignIpfsUrlResourceParser,
    rawCidAddressParser: CidUrlResourceParser,
    httpUrlParser: HttpUrlResourceParser,
    customParsersPackage: List<UrlResourceParser<UrlResource>> = emptyList()
) {

    private val defaultAddressParsers: List<UrlResourceParser<UrlResource>> =
        listOf(
            arweaveUrlParser,
            abstractIpfsAddressParser,
            foreignIpfsUrlAddressParser,
            rawCidAddressParser,
            httpUrlParser
        )

    val addressParsers = customParsersPackage.ifEmpty { defaultAddressParsers }
}
