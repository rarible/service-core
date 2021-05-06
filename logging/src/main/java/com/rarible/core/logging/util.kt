package com.rarible.core.logging

import com.rarible.core.logging.LoggerContext.addToContext
import com.rarible.core.logging.LoggingUtils.extractMDCMap
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
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

    @JvmStatic
    val MDC_MAP: Mono<Map<String, String>> = Mono.subscriberContext()
        .map { extractMDCMap(it) }
        .switchIfEmpty { Mono.just(emptyMap()) }
}
