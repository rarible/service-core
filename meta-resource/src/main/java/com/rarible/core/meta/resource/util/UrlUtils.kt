package com.rarible.core.meta.resource.util

import java.net.URI

fun String.isHttpUrl(): Boolean =
    try {
        val url = URI(this)
        (url.scheme == "http" || url.scheme == "https")
    } catch (e: Exception) {
        false
    }

fun String.removeLeadingSlashes(): String {
    var result = this
    while (result.startsWith('/')) {
        result = result.trimStart('/')
    }
    return result
}

fun URI.extension(): String = this.toString().substringAfterLast(".")
