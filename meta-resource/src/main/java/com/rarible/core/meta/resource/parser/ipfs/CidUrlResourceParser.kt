package com.rarible.core.meta.resource.parser.ipfs

import com.rarible.core.meta.resource.cid.CidValidator
import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.parser.UrlResourceParser

class CidUrlResourceParser(
    private val cidOneValidator: CidValidator
) : UrlResourceParser<IpfsUrl> {

    override fun parse(url: String): IpfsUrl? {
        val cid = url.substringBefore("/")
        if (cidOneValidator.isCid(cid)) {
            return IpfsUrl(
                originalGateway = null,
                original = url,
                path = url
            )
        }
        return null
    }
}
