package com.rarible.core.meta.resolver

import com.fasterxml.jackson.databind.node.ObjectNode

class RawMeta(
    val parsed: ObjectNode?,
    val bytes: ByteArray?,
    val mimeType: String?
) {
    companion object {
        val EMPTY = RawMeta(null, null, null)
    }
}
