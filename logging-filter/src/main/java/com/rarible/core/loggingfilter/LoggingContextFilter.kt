package com.rarible.core.loggingfilter

import com.google.common.base.CaseFormat
import com.rarible.core.logging.loggerContext
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

class LoggingContextFilter : WebFilter {
    override fun filter(ex: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return chain.filter(ex)
            .loggerContext(ex.request.headers.toLoggingContext())
    }
}

fun MultiValueMap<String, String>.toLoggingContext(): Map<String, String> {
    val result = mapNotNull { entry ->
        val key = entry.key.toLowerCase()
        if (key.startsWith(X_LOG)) {
            entry.value.firstOrNull()?.let { value ->
                CONVERTER.convert(key.substring(X_LOG.length))!! to value
            }
        } else {
            null
        }
    }.toMap()

    return if (!result.containsKey(TRACE_ID)) {
        result + (TRACE_ID to UUID.randomUUID().toString().replace("-", ""))
    } else {
        result
    }
}

const val TRACE_ID = "trace.id"
private const val X_LOG = "x-log-"
private val CONVERTER = CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.LOWER_CAMEL)
