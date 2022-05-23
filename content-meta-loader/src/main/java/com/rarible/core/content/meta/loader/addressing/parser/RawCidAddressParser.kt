package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.RawCidAddress
import com.rarible.core.content.meta.loader.addressing.SLASH
import com.rarible.core.content.meta.loader.addressing.cid.CidValidator

class RawCidAddressParser(
    private val cidOneValidator: CidValidator
) : AddressParser<RawCidAddress> {

    override fun parse(url: String): RawCidAddress? {
        val cid = url.substringBefore(SLASH)
        if (cidOneValidator.isCid(cid)) {
            return RawCidAddress(
                origin = url,
                cid = cid,
                additionalPath = url.substring(cid.length).ifEmpty { null }
            )
        }
        return null
    }
}
