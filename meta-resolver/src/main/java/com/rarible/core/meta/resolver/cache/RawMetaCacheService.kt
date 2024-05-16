package com.rarible.core.meta.resolver.cache

import com.rarible.core.meta.resource.model.UrlResource

class RawMetaCacheService(
    private val caches: List<RawMetaCache>
) {

    fun getCache(urlResource: UrlResource): RawMetaCache? {
        return caches.find { it.isSupported(urlResource) }
    }
}
