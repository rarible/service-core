package com.rarible.core.telemetry.actuator

import io.micrometer.core.instrument.Tag
import org.springframework.boot.actuate.metrics.web.reactive.server.WebFluxTagsContributor
import org.springframework.web.server.ServerWebExchange

/**
 * Client name Tag contributor for the API metrics. Required to distinguish API calls from different clients.
 * Basically, we want to see 'rarible'/'other' distribution. In the future, we may extend set of clients to track.
 * We need to be VERY careful here in terms of clientName's possible values set. Prometheus might not be able
 * to handle them.
 */
class WebRequestClientTagContributor(
    private val clientNameHeader: String = "x-rarible-client",
    // By default, we're tracking only 'rarible' client, all others will be tagged as 'other'
    private val clientNameFilter: (String) -> Boolean = { isAcceptableClientName(it) },
    // Default simple implementation to detect requests from marketplace by origin
    private val clientNameExtractor: (String) -> String? = { origin -> getClientNameFromOrigin(origin) }
) : WebFluxTagsContributor {

    override fun httpRequestTags(exchange: ServerWebExchange?, ex: Throwable?): MutableIterable<Tag>? {
        exchange ?: return toClientTag(OTHER)

        val clientName = exchange.request.headers.getFirst(clientNameHeader)
        clientName?.let { return toClientTag(it) }

        val originClientName = exchange.request.headers.origin?.let { clientNameExtractor(it) } ?: OTHER
        return toClientTag(originClientName)
    }

    private fun toClientTag(clientName: String): MutableIterable<Tag> {
        val result = if (clientNameFilter(clientName)) {
            clientName
        } else {
            OTHER
        }
        return mutableListOf(Tag.of("client", result))
    }

    companion object {

        private const val OTHER = "other"
        private const val RARIBLE = "rarible"
        private const val RARIBLE_PROTOCOL = "rarible-protocol"

        private val acceptableClientNames = setOf(RARIBLE, RARIBLE_PROTOCOL)

        private fun getClientNameFromOrigin(origin: String): String? {
            return if (origin.endsWith("rarible.com")) {
                RARIBLE
            } else {
                null
            }
        }

        private fun isAcceptableClientName(clientName: String): Boolean {
            return acceptableClientNames.contains(clientName)
        }
    }

}