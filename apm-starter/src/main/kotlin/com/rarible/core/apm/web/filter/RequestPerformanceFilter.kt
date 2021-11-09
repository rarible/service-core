package com.rarible.core.apm.web.filter

import co.elastic.apm.api.HeaderExtractor
import co.elastic.apm.api.HeadersExtractor
import com.rarible.core.apm.withTransaction
import com.rarible.core.application.ApplicationInfo
import org.springframework.core.Ordered
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class RequestPerformanceFilter(
    applicationInfo: ApplicationInfo
) : WebFilter, Ordered {

    private val service = applicationInfo.serviceName

    override fun getOrder() = Ordered.LOWEST_PRECEDENCE

    override fun filter(exchange: ServerWebExchange , chain: WebFilterChain): Mono<Void> {
        val request = exchange.request

        val headers = request.headers

        val path = request.path.pathWithinApplication().value()
        val method = request.methodValue

        return chain.filter(exchange)
            .withTransaction(
                "$service#$method#$path",
                headerExtractor = HeaderExtractor { header -> headers.getFirst(header) },
                headersExtractor = HeadersExtractor { header -> headers[header] }
            )
    }
}
