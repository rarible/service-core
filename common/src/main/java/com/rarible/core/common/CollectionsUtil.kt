@file:Suppress("SpellCheckingInspection")

package com.rarible.core.common

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async(context = this.coroutineContext) { f(it) } }.awaitAll()
}

suspend fun <A, B> Iterable<A>.pflatMap(f: suspend (A) -> Iterable<B>): List<B> = coroutineScope {
    pmap(f).flatten()
}

suspend fun <K, A, B> Map<K, A>.pmap(f: suspend (Map.Entry<K, A>) -> B): List<B> = coroutineScope {
    map { async(context = this.coroutineContext) { f(it) } }.awaitAll()
}

suspend fun <K, A, B> Map<K, A>.pflatMap(f: suspend (Map.Entry<K, A>) -> Iterable<B>): List<B> = coroutineScope {
    pmap(f).flatten()
}