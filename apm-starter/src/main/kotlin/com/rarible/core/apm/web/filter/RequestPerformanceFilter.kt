package com.rarible.core.apm.web.filter

import co.elastic.apm.api.HeaderExtractor
import co.elastic.apm.api.HeadersExtractor
import com.rarible.core.apm.withTransaction
import com.rarible.core.common.orNull
import com.rarible.core.common.toOptional
import org.springframework.core.Ordered
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RequestPerformanceFilter(
    private val dispatcherHandler: DispatcherHandler
) : WebFilter, Ordered {

    override fun getOrder() = Ordered.LOWEST_PRECEDENCE

    override fun filter(exchange: ServerWebExchange , chain: WebFilterChain): Mono<Void> {
        val request = exchange.request

        val headers = request.headers

        val path = request.path.pathWithinApplication().value()
        val method = request.methodValue

        return Flux.fromIterable(dispatcherHandler.handlerMappings)
            .flatMap { it.getHandler(exchange) }.next().toOptional()
            .flatMap {
                val handler = it.orNull()
                val defaultName = "$method $path"
                val (name, labels) = if (handler is HandlerMethod) {
                    "${handler.beanType.simpleName}#${handler.method.name}" to listOf("request" to defaultName)
                } else {
                    defaultName to emptyList()
                }
                chain.filter(exchange)
                    .withTransaction(
                        name = name,
                        labels = labels,
                        headerExtractor = HeaderExtractor { header -> headers.getFirst(header) },
                        headersExtractor = HeadersExtractor { header -> headers[header] }
                    )
            }
    }
}
