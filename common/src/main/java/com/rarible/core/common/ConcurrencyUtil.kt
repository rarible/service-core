package com.rarible.core.common

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <T, R> Collection<T>.asyncBatchHandle(batch: Int, handler: suspend (T) -> R): List<R> {
    val list = this
    return coroutineScope {
        list.chunked(batch).map { chunk ->
            chunk.map { element ->
                async {
                    handler(element)
                }
            }.awaitAll()
        }.flatten()
    }
}