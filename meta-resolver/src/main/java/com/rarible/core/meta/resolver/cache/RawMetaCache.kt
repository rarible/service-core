package com.rarible.core.meta.resolver.cache

import com.rarible.core.meta.resource.model.UrlResource

interface RawMetaCache {

    fun isSupported(urlResource: UrlResource): Boolean

    suspend fun get(urlResource: UrlResource): RawMetaEntry?

    suspend fun save(urlResource: UrlResource, content: String): RawMetaEntry
}
