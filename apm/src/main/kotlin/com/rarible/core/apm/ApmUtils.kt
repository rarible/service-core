@file:Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE", "EXPERIMENTAL_API_USAGE")

package com.rarible.core.apm

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Span
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import reactor.util.context.Context
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
        body = body
    )
}

suspend fun <T> withSpan(
    name: String,
    type: String,
    subType: String? = null,
    action: String? = null,
    body: suspend () -> T
): T {
    val ctx = getApmContext()
    return if (ctx != null) {
        val span = ctx.span.startSpan(type, subType, action)
        span.setName(name)
        span.using(body)
    } else {
        body()
    }
}

suspend fun <T> withTransaction(name: String, body: suspend () -> T): T {
    val transaction = ElasticApm.startTransaction()
    transaction.setName(name)

    return transaction.using(body)
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