package com.rarible.core.meta.resolver.url

/**
 * Custom sanitizers for specific cases
 */
interface MetaUrlSanitizer<K> {

    fun sanitize(entityId: K, metaUrl: String): String?
}
