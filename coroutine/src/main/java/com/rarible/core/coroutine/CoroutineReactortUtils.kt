package com.rarible.core.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

fun <T> coroutineToMono(func: suspend CoroutineScope.() -> T?): Mono<T> {
    return Mono.subscriberContext().flatMap { ctx ->
        mono(ctx.toCoroutineContext(), func)
    }
}

fun <T> coroutineToFlux(func: suspend CoroutineScope.() -> Iterable<T>): Flux<T> {
    return Mono.subscriberContext().flatMap { ctx ->
        mono(ctx.toCoroutineContext(), func)
    }.flatMapIterable { it }
}

fun Context.toCoroutineContext(): CoroutineContext {
    return this.stream()
        .filter { it.value is CoroutineContext }
        .map { it.value as CoroutineContext }
        .reduce { context1, context2 -> context1 + context2 }
        .orElse(EmptyCoroutineContext)
}

suspend fun <T> Mono<T>.awaitFirstWithContext(contextHandler: (CoroutineContext, Context) -> Context): T {
    val ctx = coroutineContext
    return this
        .subscriberContext { contextHandler(ctx, it) }
        .awaitFirst()
}
