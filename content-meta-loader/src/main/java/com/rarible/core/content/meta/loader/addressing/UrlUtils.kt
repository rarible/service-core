package com.rarible.core.content.meta.loader.addressing

import java.net.URL

fun String.isValidUrl(): Boolean =
    try {
        val url = URL(this)
        (url.protocol == "http" || url.protocol == "https")
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

const val SLASH = "/"
