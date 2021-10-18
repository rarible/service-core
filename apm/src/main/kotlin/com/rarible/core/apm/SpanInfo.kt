package com.rarible.core.apm

data class SpanInfo(
    val type: String,
    val subType: String,
    val action: String,
    val name: String
)
