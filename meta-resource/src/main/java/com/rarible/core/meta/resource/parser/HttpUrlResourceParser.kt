package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.util.isHttpUrl

class HttpUrlResourceParser : UrlResourceParser<HttpUrl> {

    override fun parse(url: String): HttpUrl? {
        if (url.isHttpUrl()) {
            return HttpUrl(url)
        }
        return null
    }
}
