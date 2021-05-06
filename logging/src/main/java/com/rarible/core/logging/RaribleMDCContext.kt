package com.rarible.core.logging

import com.rarible.core.logging.LoggingUtils.extractMDCMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

typealias MDCContextMap = Map<String, String>?

@ExperimentalCoroutinesApi
class RaribleMDCContext : ThreadContextElement<MDCContextMap>, AbstractCoroutineContextElement(RaribleMDCContext) {

    companion object Key : CoroutineContext.Key<RaribleMDCContext>

    override fun updateThreadContext(context: CoroutineContext): MDCContextMap {
        val oldState = MDC.getCopyOfContextMap()
        val reactorContext = context[ReactorContext]
        if (reactorContext != null) {
            val map = extractMDCMap(reactorContext.context)
            setCurrent(map)
        }
        return oldState
    }

    /** @suppress */
    override fun restoreThreadContext(context: CoroutineContext, oldState: MDCContextMap) {
        setCurrent(oldState)
    }

    private fun setCurrent(contextMap: MDCContextMap) {
        if (contextMap == null) {
            MDC.clear()
        } else {
            MDC.setContextMap(contextMap)
        }
    }
}

@ExperimentalCoroutinesApi
suspend fun <T> withMdc(block: suspend CoroutineScope.() -> T): T {
    return withContext(RaribleMDCContext(), block)
}