package com.rarible.core.common

import java.io.IOException

suspend fun <T> wrapWithIOException(block: suspend () -> T): T {
    try {
        return block()
    } catch (e: Exception) {
        if (e is IOException) {
            throw e
        } else {
            throw IOException(e)
        }
    }
}
