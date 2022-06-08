package com.rarible.core.common

fun String?.safeSplit(vararg delimiters: String = arrayOf(",")): List<String> {
    return this?.split(*delimiters)?.filter { it.isNotBlank() } ?: emptyList()
}

fun String?.ifNotBlank() = this?.takeIf { it.isNotBlank() }
