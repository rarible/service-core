package com.rarible.core.meta.resolver.url

import com.rarible.core.meta.resource.model.UrlResource

/**
 * URL parser - in most cases default implementation is fine, but for some
 * specific cases might be overridden (actual for some custom resolvers for Ethereum collections)
 */
interface MetaUrlParser<K> {

    fun parse(entityId: K, metaUrl: String): UrlResource
}
