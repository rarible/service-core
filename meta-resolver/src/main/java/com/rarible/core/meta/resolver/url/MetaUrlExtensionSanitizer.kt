package com.rarible.core.meta.resolver.url

class MetaUrlExtensionSanitizer<K>(
    private val extensions: Set<String> = setOf(".json")
) : MetaUrlSanitizer<K> {

    override fun sanitize(entityId: K, metaUrl: String): String? {
        extensions.forEach {
            if (metaUrl.endsWith(it)) {
                return metaUrl.substring(0, metaUrl.length - it.length)
            }
        }
        return null
    }
}
