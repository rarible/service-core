package com.rarible.core.meta.resolver.util

import com.fasterxml.jackson.databind.JsonNode

fun JsonNode.getText(vararg paths: List<String>): String? {
    for (path in paths) {
        val current = path.fold(this) { node, subPath -> node.path(subPath) }
        if (current.isTextual || current.isNumber) {
            return current.asText()
        }
    }
    return null
}

fun JsonNode.getArray(path: List<String>): Iterable<JsonNode>? {
    val current = path.fold(this) { node, subPath -> node.path(subPath) }
    if (current.isArray) {
        return current
    }
    return null
}

fun JsonNode.getText(vararg paths: String): String? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isTextual || current.isNumber || current.isBoolean) {
            return current.asText()
        }
    }
    return null
}

fun JsonNode.getInt(vararg paths: String): Int? {
    for (path in paths) {
        val current = this.path(path)
        if (current.isInt) {
            return current.asInt()
        }
    }
    return null
}
