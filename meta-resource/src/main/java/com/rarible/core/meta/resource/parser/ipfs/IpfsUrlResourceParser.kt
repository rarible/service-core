package com.rarible.core.meta.resource.parser.ipfs

import com.rarible.core.meta.resource.cid.CidV1Validator
import com.rarible.core.meta.resource.cid.CidValidator
import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.parser.UrlResourceParser

class IpfsUrlResourceParser(
    cidValidator: CidValidator = CidV1Validator
) : UrlResourceParser<IpfsUrl> {

    val parser: List<UrlResourceParser<IpfsUrl>> = listOf(
        AbstractIpfsUrlResourceParser(),
        ForeignIpfsUrlResourceParser(cidValidator),
        CidUrlResourceParser(cidValidator)
    )

    override fun parse(url: String): IpfsUrl? {
        return parser.firstNotNullOfOrNull { it.parse(url) }
    }
}