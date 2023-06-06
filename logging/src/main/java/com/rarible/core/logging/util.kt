package com.rarible.core.logging

import com.rarible.core.logging.LoggerContext.addToContext
import com.rarible.core.logging.LoggingUtils.extractMDCMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.util.context.Context
import java.util.UUID
import kotlin.coroutines.EmptyCoroutineContext

const val TRACE_ID = "trace.id"
const val BATCH_ID = "batch.id"

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


fun generateTraceId() = UUID.randomUUID().toString().replace("-", "")

suspend inline fun <T> withTraceId(crossinline block: suspend CoroutineScope.() -> T): T = withParamId(TRACE_ID, block)

suspend inline fun <T> withBatchId(crossinline block: suspend CoroutineScope.() -> T): T = withParamId(BATCH_ID, block)

suspend inline fun <T> withParamId(param: String, crossinline block: suspend CoroutineScope.() -> T): T {
    val traceId = MDC.get(param) ?: generateTraceId()
    val currentContext = MDC.getCopyOfContextMap() ?: HashMap()
    currentContext[param] = traceId
    return withContext(MDCContext(currentContext)) {
        block()
    }
}

fun <T> Flow<T>.withTraceId(): Flow<T> {
    val traceId = MDC.get(TRACE_ID) ?: generateTraceId()
    val currentContext = MDC.getCopyOfContextMap() ?: HashMap()
    currentContext[TRACE_ID] = traceId
    return flowOn(
        MDCContext(currentContext)
    )
}

@Suppress("DeferredIsResult")
fun <T> CoroutineScope.asyncWithTraceId(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    context: kotlin.coroutines.CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T
): Deferred<T> = async(context = MDCContext() + context, start = start, block = block)

fun CoroutineScope.launchWithTraceId(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(context = MDCContext(), start = start, block = block)