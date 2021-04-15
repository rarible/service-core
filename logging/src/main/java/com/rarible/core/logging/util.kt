package com.rarible.core.logging

import com.rarible.core.logging.LoggingUtils.CONTEXT_NAME
import kotlinx.coroutines.slf4j.MDCContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.*

fun <T> Mono<T>.loggerContext(key: String, value: String): Mono<T> =
    subscriberContext { addToContext(it, mapOf(key to value) ) }

fun <T> Flux<T>.loggerContext(key: String, value: String): Flux<T> =
    subscriberContext { addToContext(it, mapOf(key to value) ) }

fun <T> Mono<T>.loggerContext(map: Map<String, String>): Mono<T> =
    subscriberContext { addToContext(it, map) }

fun <T> Flux<T>.loggerContext(map: Map<String, String>): Flux<T> =
    subscriberContext { addToContext(it, map) }

fun addToContext(ctx: Context, map: Map<String, String>): Context {
    val newMdcContext = if (ctx.hasKey(CONTEXT_NAME)) {
        val mdcContext: MDCContext = ctx.get(CONTEXT_NAME)
        MDCContext(mdcContext.contextMap?.plus(map) ?: map)
    } else {
        MDCContext(map)
    }
    return ctx.put(CONTEXT_NAME, newMdcContext)
}

val MDC_CONTEXT: Mono<Optional<MDCContext>> = Mono.subscriberContext()
    .filter { it.hasKey(CONTEXT_NAME) }
    .map { it.getOrEmpty<MDCContext>(CONTEXT_NAME) }