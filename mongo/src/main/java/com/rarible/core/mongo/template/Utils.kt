package com.rarible.core.mongo.template

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.withContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context
import kotlin.coroutines.coroutineContext

suspend inline fun <T> withReadReplica(crossinline block: suspend CoroutineScope.() -> T): T {
    val currentContext = coroutineContext[ReactorContext] ?: ReactorContext(Context.empty())
    val updatedContext = currentContext.context.put(RaribleReactiveMongoTemplate.USE_REPLICA_CONTEXT_KEY, true)
    return withContext(ReactorContext(updatedContext)) {
        block()
    }
}

fun <T> Flow<T>.withReadReplica(): Flow<T> = flowOn(
    Context.of(RaribleReactiveMongoTemplate.USE_REPLICA_CONTEXT_KEY, true)
        .asCoroutineContext()
)

fun <T> Mono<T>.withReadReplica(): Mono<T> = contextWrite(
    Context.of(RaribleReactiveMongoTemplate.USE_REPLICA_CONTEXT_KEY, true)
)

fun <T> Flux<T>.withReadReplica(): Flux<T> = contextWrite(
    Context.of(RaribleReactiveMongoTemplate.USE_REPLICA_CONTEXT_KEY, true)
)
