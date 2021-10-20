@file:Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE", "EXPERIMENTAL_API_USAGE")

package com.rarible.core.apm

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.HeaderExtractor
import co.elastic.apm.api.HeadersExtractor
import co.elastic.apm.api.Span
import co.elastic.apm.api.Transaction
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.Optional
import kotlin.coroutines.coroutineContext

suspend fun <T> withSpan(
    info: SpanInfo,
    body: suspend () -> T
): T {
    return withSpan(
        type = info.type,
        subType = info.subType,
        action = info.subType,
        name = info.name,
        labels = info.labels,
        body = body
    )
}

suspend fun <T> withSpan(
    name: String,
    type: String? = null,
    subType: String? = null,
    action: String? = null,
    labels: List<Pair<String, Any>> = emptyList(),
    body: suspend () -> T
): T {
    val ctx = getApmContext()
    return if (ctx != null) {
        ctx.span
            .createSpan(name, type, subType, action, labels)
            .using(body)
    } else {
        body()
    }
}

suspend fun <T> withTransaction(
    name: String,
    labels: List<Pair<String, Any>> = emptyList(),
    headerExtractor: HeaderExtractor? = null,
    headersExtractor: HeadersExtractor? = null,
    body: suspend () -> T
): T {
    return createTransaction(name, labels, headerExtractor, headersExtractor)
        .using(body)
}

suspend fun <T> Span.using(body: suspend () -> T): T {
    return try {
        val current = coroutineContext[ReactorContext.Key]?.context ?: Context.empty()
        withContext(ReactorContext(current.put(ApmContext.Key, ApmContext(this)))) {
            body()
        }
    } catch (e: Throwable) {
        captureException(e)
        throw e
    } finally {
        end()
    }
}

suspend fun getApmContext(): ApmContext? {
    val ctx = coroutineContext[ReactorContext.Key]?.context
    return if (ctx != null && ctx.hasKey(ApmContext.Key)) {
        ctx.get(ApmContext.Key)
    } else {
        null
    }
}

private val apmContext: Mono<ApmContext> =
    Mono.subscriberContext()
        .flatMap {
            if (it.hasKey(ApmContext.Key)) {
                Mono.just(it.get<ApmContext>(ApmContext.Key))
            } else {
                Mono.empty()
            }
        }

fun <T> Mono<T>.withSpan(
    name: String,
    type: String? = null,
    subType: String? = null,
    action: String? = null,
    labels: List<Pair<String, Any>> = emptyList()
): Mono<T> {
    return apmContext
        .map { it.span.createSpan(name, type, subType, action, labels) }
        .using(this)
}

fun <T> Mono<T>.withTransaction(
    name: String,
    labels: List<Pair<String, Any>> = emptyList(),
    headerExtractor: HeaderExtractor? = null,
    headersExtractor: HeadersExtractor? = null
): Mono<T> {
    return Mono
        .defer { Mono.just(createTransaction(name, labels, headerExtractor, headersExtractor)) }
        .using(this)
}

private fun <T> Mono<out Span>.using(mono: Mono<T>): Mono<T> {
    return this
        .map { Optional.of(it) }
        .switchIfEmpty(Mono.just(Optional.empty()))
        .flatMap { opt ->
            if (opt.isPresent) {
                val span = opt.get()
                mono.doOnEach {
                    when {
                        it.isOnError -> span.captureException(it.throwable)
                        it.isOnComplete -> span.end()
                    }
                }.subscriberContext { it.put(ApmContext.Key, ApmContext(span)) }
            } else {
                mono
            }
        }
}

private fun Span.createSpan(
    name: String,
    type: String? = null,
    subType: String? = null,
    action: String? = null,
    labels: List<Pair<String, Any>> = emptyList()
): Span {
    val span = this.startSpan(type, subType, action)
    span.setName(name)
    span.setLabels(labels)
    return span
}

private fun createTransaction(
    name: String,
    labels: List<Pair<String, Any>> = emptyList(),
    headerExtractor: HeaderExtractor? = null,
    headersExtractor: HeadersExtractor? = null
): Transaction {
    val span = if (headerExtractor != null && headersExtractor != null) {
        ElasticApm.startTransactionWithRemoteParent(headerExtractor, headersExtractor)
    } else if (headerExtractor != null) {
        ElasticApm.startTransactionWithRemoteParent(headerExtractor)
    } else {
        ElasticApm.startTransaction()
    }
    span.setName(name)
    span.setLabels(labels)
    return span
}

private fun Span.setLabels(labels: List<Pair<String, Any>> = emptyList()) {
    for ((key, value) in labels) {
        when (value) {
            is Number -> setLabel(key, value)
            is String -> setLabel(key, value)
            is Boolean -> setLabel(key, value)
        }
    }
}
