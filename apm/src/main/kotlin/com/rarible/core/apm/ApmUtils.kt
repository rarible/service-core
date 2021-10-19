@file:Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")

package com.rarible.core.apm

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Span
import kotlinx.coroutines.withContext
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
    val ctx = coroutineContext[ApmContext.Key]

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

    return withContext(ApmContext(transaction)) {
        transaction.using(body)
    }
}

suspend inline fun <T> Span.using(body: suspend () -> T): T {
    return try {
        body()
    } catch (e: Throwable) {
        captureException(e)
        throw e
    } finally {
        end()
    }
}
