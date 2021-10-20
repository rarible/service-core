package com.rarible.core.apm

data class SpanInfo(
    val name: String,
    val type: String? = null,
    val subType: String? = null,
    val action: String? = null,
    val labels: List<Pair<String, Any>> = emptyList()
)
