package com.rarible.core.apm

data class SpanInfo(
    val name: String,
    val type: String,
    val subType: String? = null,
    val action: String? = null
)
