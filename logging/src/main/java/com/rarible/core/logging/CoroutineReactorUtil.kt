package com.rarible.core.logging

import com.rarible.core.coroutine.awaitFirstWithContext
import com.rarible.core.logging.LoggingUtils.CONTEXT_NAME
import kotlinx.coroutines.slf4j.MDCContext
import reactor.core.publisher.Mono

suspend fun <T> Mono<T>.awaitFirstWithMDC(): T {
    return awaitFirstWithContext { coroutineContext, context ->
        val mdc = coroutineContext[MDCContext]
        if (mdc != null) {
            context.put(CONTEXT_NAME, mdc)
        } else {
            context
        }
    }
}
