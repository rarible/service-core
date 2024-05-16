package com.rarible.core.meta.resolver.url

import com.rarible.core.meta.resolver.MetaResolverException
import com.rarible.core.meta.resource.model.UrlResource

class DefaultMetaUrlParser<K>(
    private val urlService: UrlService
) : MetaUrlParser<K> {

    override fun parse(entityId: K, metaUrl: String): UrlResource {
        return urlService.parseUrl(metaUrl, entityId.toString()) ?: throw MetaResolverException(
            "$metaUrl is corrupted",
            MetaResolverException.Status.CORRUPTED_URL
        )
    }
}
