package com.rarible.core.apm

import kotlin.coroutines.coroutineContext

suspend fun <T> span(
    info: SpanInfo,
    body: suspend () -> T
): T {
    return span(
        type = info.type,
        subType = info.subType,
        action = info.subType,
        name = info.name,
        body = body
    )
}

suspend fun <T> span(
    type: String,
    subType: String,
    action: String,
    name: String,
    body: suspend () -> T
): T {
    val ctx = coroutineContext[ApmContext.Key]

    return if (ctx != null) {
        val span = ctx.span.startSpan(type, subType, action)
        span.setName(name)
        try {
            body()
        } catch (e: Throwable) {
            span.captureException(e)
            throw e
        } finally {
            span.end()
        }
    } else {
        body()
    }
}
