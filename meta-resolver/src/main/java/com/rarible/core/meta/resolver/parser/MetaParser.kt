package com.rarible.core.meta.resolver.parser

import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Raw metadata parser to JSON
 */
interface MetaParser<K> {

    fun parse(entityId: K, json: String): ObjectNode
}
