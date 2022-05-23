package com.rarible.core.content.meta.loader.addressing.parser

import com.rarible.core.content.meta.loader.addressing.HttpUrl
import com.rarible.core.content.meta.loader.addressing.isHttpUrl

class HttpUrlResourceParser : UrlResourceParser<HttpUrl> {

    override fun parse(url: String): HttpUrl? {
        if (url.isHttpUrl()) {
            return HttpUrl(url)
        }
        return null
    }
}
