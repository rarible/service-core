package ru.roborox.logging.utils

import kotlinx.coroutines.slf4j.MDCContext
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

fun addToContext(ctx: Context, map: Map<String, String>): Context {
    val newMdcContext = if (ctx.hasKey(MDC_CONTEXT)) {
        val mdcContext: MDCContext = ctx.get(MDC_CONTEXT)
        MDCContext(mdcContext.contextMap?.plus(map) ?: map)
    } else {
        MDCContext(map)
    }
    return ctx.put(MDC_CONTEXT, newMdcContext)
}

val MDC_CONTEXT: String = MDCContext::class.java.simpleName