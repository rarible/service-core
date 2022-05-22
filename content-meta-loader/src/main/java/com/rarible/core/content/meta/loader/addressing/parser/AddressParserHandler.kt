package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.ResourceAddress

class AddressParserHandler(
    private val addressParserProvider: AddressParserProvider
) {

    fun parse(address: String): ResourceAddress? {
        for (parser in addressParserProvider.addressParsers) {
            val resourceAddress = parser.parse(address)
            if (resourceAddress != null) {
                return resourceAddress
            }
        }
        return null
    }
}

class AddressParserProvider(
    arweaveUrlParser: ArweaveAddressParser,
    ipfsAddressParser: IpfsAddressParser,
    rawCidAddressParser: RawCidAddressParser,
    httpUrlParser: HttpAddressParser,
    customParsersPackage: List<AddressParser> = emptyList()
) {

    private val defaultAddressParsers: List<AddressParser> =
        listOf(
            arweaveUrlParser,
            ipfsAddressParser,
            rawCidAddressParser,
            httpUrlParser
        )

    val addressParsers = customParsersPackage.ifEmpty { defaultAddressParsers }
}
