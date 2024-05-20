package com.rarible.core.meta.resolver

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Metadata mapper from raw JSON to some DTO
 */
interface MetaMapper<K, M> {

    fun map(entityId: K, json: ObjectNode): M

    fun isEmpty(meta: M): Boolean
}
