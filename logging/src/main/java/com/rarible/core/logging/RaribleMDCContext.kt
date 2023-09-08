package com.rarible.core.logging

import com.rarible.core.logging.LoggingUtils.extractMDCMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

typealias MDCContextMap = Map<String, String>?

class RaribleMDCContext(
    val contextMap: MDCContextMap = MDC.getCopyOfContextMap()
) : ThreadContextElement<MDCContextMap>, AbstractCoroutineContextElement(RaribleMDCContext) {

    companion object Key : CoroutineContext.Key<RaribleMDCContext>

    override fun updateThreadContext(context: CoroutineContext): MDCContextMap {
        val oldState = MDC.getCopyOfContextMap()
        val reactorContext = context[ReactorContext]
        if (reactorContext != null) {
            val map = extractMDCMap(reactorContext.context)
            val result = HashMap(map)
            if (contextMap != null) {
                result.putAll(contextMap)
            }
            setCurrent(result)
        } else {
            setCurrent(contextMap)
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

suspend fun <T> withMdc(block: suspend CoroutineScope.() -> T): T {
    return withContext(RaribleMDCContext(), block)
}

suspend fun <T> addToMdc(vararg values: Pair<String, String>, block: suspend CoroutineScope.() -> T): T {
    val map = MDC.getCopyOfContextMap()
    val newValues = mapOf(*values)
    val resultMap = if (map == null) {
        newValues
    } else {
        newValues + map
    }
    return withContext(RaribleMDCContext(resultMap), block)
}
