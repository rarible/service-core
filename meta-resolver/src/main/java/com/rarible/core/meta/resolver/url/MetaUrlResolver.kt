package com.rarible.core.meta.resolver.url

/**
 * NFT/Collection metadata URL resolver, in most cases get it from blockchain
 */
interface MetaUrlResolver<K> {

    fun getUrl(entityId: K): String?
}
