@file:Suppress("SpellCheckingInspection")

package com.rarible.core.common

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <A, B> Iterable<A>.mapAsync(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

suspend fun <A, B> Iterable<A>.flatMapAsync(f: suspend (A) -> Iterable<B>): List<B> = coroutineScope {
    mapAsync(f).flatten()
}

suspend fun <K, A, B> Map<K, A>.mapAsync(f: suspend (Map.Entry<K, A>) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

suspend fun <K, A, B> Map<K, A>.flatMapAsync(f: suspend (Map.Entry<K, A>) -> Iterable<B>): List<B> = coroutineScope {
    mapAsync(f).flatten()
}
