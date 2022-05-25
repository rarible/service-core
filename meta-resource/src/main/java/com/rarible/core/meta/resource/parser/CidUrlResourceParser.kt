package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.Cid
import com.rarible.core.meta.resource.cid.CidValidator

class CidUrlResourceParser(
    private val cidOneValidator: CidValidator
) : UrlResourceParser<Cid> {

    override fun parse(url: String): Cid? {
        val cid = url.substringBefore("/")
        if (cidOneValidator.isCid(cid)) {
            return Cid(
                original = url,
                cid = cid,
                subPath = url.substring(cid.length).ifEmpty { null }
            )
        }
        return null
    }
}
