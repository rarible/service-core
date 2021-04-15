package com.rarible.core.logging

import com.rarible.core.coroutine.awaitFirstWithContext
import kotlinx.coroutines.slf4j.MDCContext
import reactor.core.publisher.Mono
import ru.roborox.logging.utils.MDC_CONTEXT

suspend fun <T> Mono<T>.awaitFirstWithMDC(): T {
    return awaitFirstWithContext { coroutineContext, context ->
        val mdc = coroutineContext[MDCContext]
        if (mdc != null) {
            context.put(MDC_CONTEXT, mdc)
        } else {
            context
        }
    }
}
