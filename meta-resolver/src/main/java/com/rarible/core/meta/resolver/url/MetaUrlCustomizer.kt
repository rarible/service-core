package com.rarible.core.meta.resolver.url

/**
 * Metadata URL customizer. Can be used for substitutions in URLs like http://test.com/0xab54792bc/{id},
 * where {id} placeholder should be replaced by tokenId
 */
interface MetaUrlCustomizer<K> {

    fun customize(entityId: K, metaUrl: String): String?
}
