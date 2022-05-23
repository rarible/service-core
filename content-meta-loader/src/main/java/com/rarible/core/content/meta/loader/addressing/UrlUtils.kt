package com.rarible.core.content.meta.loader.addressing

import java.net.URL

fun String.isHttpUrl(): Boolean =
    try {
        val url = URL(this)
        (url.protocol == "http" || url.protocol == "https")
    } catch (e: Exception) {
        false
    }

fun String.removeLeadingSlashes(): String {
    var result = this
    while (result.startsWith(SLASH)) {
        result = result.trimStart(SLASH)
    }
    return result
}

// TODO remove
const val SLASH = '/'
