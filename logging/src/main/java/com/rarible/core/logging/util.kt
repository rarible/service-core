package com.rarible.core.logging

import com.rarible.core.logging.LoggerContext.addToContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context

fun <T> Mono<T>.loggerContext(key: String, value: String): Mono<T> =
    subscriberContext { addToContext(it, mapOf(key to value) ) }

fun <T> Flux<T>.loggerContext(key: String, value: String): Flux<T> =
    subscriberContext { addToContext(it, mapOf(key to value) ) }

fun <T> Mono<T>.loggerContext(map: Map<String, String>): Mono<T> =
    subscriberContext { addToContext(it, map) }

fun <T> Flux<T>.loggerContext(map: Map<String, String>): Flux<T> =
    subscriberContext { addToContext(it, map) }

object LoggerContext {
    @JvmStatic
    fun addToContext(ctx: Context, map: Map<String, String>): Context {
        return map.toList().fold(ctx) { acc, pair ->
            acc.put(LoggingUtils.LOG_ + pair.first, pair.second)
        }
    }
}
