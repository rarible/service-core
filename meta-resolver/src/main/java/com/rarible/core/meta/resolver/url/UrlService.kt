package com.rarible.core.meta.resolver.url

import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.resolver.UrlResolver

class UrlService(
    private val urlParser: UrlParser,
    private val urlResolver: UrlResolver
) {

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInternalHttpUrl(url: String): String? {
        return urlParser.parse(url)?.let { resolveInternalHttpUrl(it) }
    }

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(url: String): String? {
        return urlParser.parse(url)?.let { resolvePublicHttpUrl(it) }
    }

    fun parseUrl(url: String, id: String): UrlResource? {
        return urlParser.parse(url)
    }

    // Used only for internal operations, such urls should NOT be stored anywhere
    fun resolveInternalHttpUrl(resource: UrlResource): String = urlResolver.resolveInternalUrl(resource)

    // Used to build url exposed to the DB cache or API responses
    fun resolvePublicHttpUrl(resource: UrlResource): String = urlResolver.resolvePublicUrl(resource)
}
