package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.ResourceAddress
import com.rarible.core.content.meta.loader.addressing.parser.ipfs.AbstractIpfsAddressParser
import com.rarible.core.content.meta.loader.addressing.parser.ipfs.ForeignIpfsUrlAddressParser

class AddressParsingProcessor(
    private val addressParserProvider: AddressParserProvider
) {

    fun parse(address: String): ResourceAddress? {
        for (parser in addressParserProvider.addressParsers) {
            val resourceAddress = parser.parse(address.trim())
            if (resourceAddress != null) {
                return resourceAddress
            }
        }
        return null
    }
}

class AddressParserProvider(
    arweaveUrlParser: ArweaveAddressParser,
    abstractIpfsAddressParser: AbstractIpfsAddressParser,
    foreignIpfsUrlAddressParser: ForeignIpfsUrlAddressParser,
    rawCidAddressParser: RawCidAddressParser,
    httpUrlParser: HttpAddressParser,
    customParsersPackage: List<AddressParser<ResourceAddress>> = emptyList()
) {

    private val defaultAddressParsers: List<AddressParser<ResourceAddress>> =
        listOf(
            arweaveUrlParser,
            abstractIpfsAddressParser,
            foreignIpfsUrlAddressParser,
            rawCidAddressParser,
            httpUrlParser
        )

    val addressParsers = customParsersPackage.ifEmpty { defaultAddressParsers }
}
