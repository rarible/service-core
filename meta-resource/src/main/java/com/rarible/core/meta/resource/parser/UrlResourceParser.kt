package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.model.UrlResource

interface UrlResourceParser<out T : UrlResource> {

    fun parse(url: String): T?
}
