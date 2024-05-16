package com.rarible.core.meta.resolver

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Metadata mapper from raw JSON to some DTO
 */
interface MetaMapper<K, M : Meta> {

    fun map(entityId: K, json: ObjectNode): M
}
