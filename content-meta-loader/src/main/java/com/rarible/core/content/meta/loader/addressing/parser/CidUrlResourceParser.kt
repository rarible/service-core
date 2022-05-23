package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.Cid
import com.rarible.core.content.meta.loader.addressing.cid.CidValidator

class CidUrlResourceParser(
    private val cidOneValidator: CidValidator
) : UrlResourceParser<Cid> {

    override fun parse(url: String): Cid? {
        val cid = url.substringBefore("/")
        if (cidOneValidator.isCid(cid)) {
            return Cid(
                original = url,
                cid = cid,
                additionalPath = url.substring(cid.length).ifEmpty { null }
            )
        }
        return null
    }
}
