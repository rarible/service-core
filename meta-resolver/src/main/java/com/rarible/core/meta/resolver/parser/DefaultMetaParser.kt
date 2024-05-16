package com.rarible.core.meta.resolver.parser

import com.fasterxml.jackson.databind.node.ObjectNode

class DefaultMetaParser<K> : MetaParser<K> {

    override fun parse(entityId: K, json: String): ObjectNode {
        return JsonMetaParser.parse(entityId.toString(), json)
    }
}
