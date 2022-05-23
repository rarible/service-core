package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.UrlResource

interface UrlResourceParser<out T : UrlResource> {

    fun parse(url: String): T?
}
