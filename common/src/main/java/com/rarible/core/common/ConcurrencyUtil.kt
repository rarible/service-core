package com.rarible.core.common

import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <T, R> Collection<T>.asyncBatchHandle(batch: Int, handler: suspend (T) -> R): List<R> {
    val list = this
    return coroutineScope {
        list.chunked(batch).map { chunk ->
            chunk.map { element ->
                asyncWithTraceId {
                    handler(element)
                }
            }.awaitAll()
        }.flatten()
    }
}
