package com.rarible.core.content.meta.loader.ipfs

import java.net.URL

fun String.isValidUrl(): Boolean =
    try {
        val url = URL(this)
        (url.protocol == "http" || url.protocol == "https")
    } catch (e: Exception) {
        false
    }

fun String.encodeHtmlUrl(): String = this.replace(" ", "%20")

fun String.removeLeadingSlashes(): String {
    var result = this
    while (result.startsWith('/')) {
        result = result.trimStart('/')
    }
    return result
}
