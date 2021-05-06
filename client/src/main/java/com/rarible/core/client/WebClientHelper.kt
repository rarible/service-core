package com.rarible.core.client

import com.google.common.base.CaseFormat
import com.rarible.core.logging.LoggerContext.MDC_MAP
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

object WebClientHelper {
    @JvmStatic
    val LOG_HEADERS: Mono<Map<String, String>> = MDC_MAP
        .map { it.toHeadersMap() }

    @JvmStatic
    fun preprocess(requestBuilder: WebClient.RequestBodySpec): Mono<WebClient.RequestBodySpec> {
        return LOG_HEADERS
            .map { preprocess(requestBuilder, it) }
    }

    private fun preprocess(requestBuilder: WebClient.RequestBodySpec, headers: Map<String, String>) =
        headers.entries.fold(requestBuilder) { rb, entry ->
            rb.header(entry.key, entry.value)
        }
}

fun Map<String, String>.toHeadersMap() =
    this.map { entry -> "x-log-${CONVERTER.convert(entry.key)}" to entry.value }
        .toMap()

private val CONVERTER = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN)
