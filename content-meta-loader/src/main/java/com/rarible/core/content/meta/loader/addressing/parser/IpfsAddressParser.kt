package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.IpfsUrl
import com.rarible.core.content.meta.loader.addressing.ResourceAddress
import com.rarible.core.content.meta.loader.addressing.parser.ipfs.AbstractIpfsUrlChecker
import com.rarible.core.content.meta.loader.addressing.parser.ipfs.ForeignIpfsUriChecker

class IpfsAddressParser(
    val foreignIpfsUriChecker: ForeignIpfsUriChecker,
    val abstractIpfsUrlChecker: AbstractIpfsUrlChecker
) : AddressParser {

    override fun parse(url: String): IpfsUrl? {
        // Checking prefixed IPFS URI like ipfs://Qmlalala
        abstractIpfsUrlChecker.check(url)
            ?.let {
                return it
            }

        // Checking if foreign IPFS url contains /ipfs/ like http://ipfs.io/ipfs/lalala  // TODO Сделать динамический набор чекеров
        foreignIpfsUriChecker.check(url)
            ?.let {
                return it
            }
        return null
    }
}
