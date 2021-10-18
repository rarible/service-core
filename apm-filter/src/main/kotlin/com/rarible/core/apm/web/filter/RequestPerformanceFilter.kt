package com.rarible.core.apm.web.filter

import co.elastic.apm.api.ElasticApm
import com.rarible.core.apm.ApmContext
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import org.springframework.core.Ordered
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class RequestPerformanceFilter(
    environmentInfo: ApplicationEnvironmentInfo,
    applicationInfo: ApplicationInfo
) : WebFilter, Ordered {

    private val environment = environmentInfo.name
    private val service = applicationInfo.serviceName

    override fun getOrder() = Ordered.LOWEST_PRECEDENCE

    override fun filter(exchange: ServerWebExchange , chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        val path = request.path.pathWithinApplication().value()
        val method = request.methodValue

        val transaction = ElasticApm.startTransaction()
        transaction.setName("$service#$method#$path")
        transaction.setLabel("env", environment)

        return chain.filter(exchange)
            .doOnSuccess {
                transaction.setResult("${response.statusCode?.name}")
            }
            .doOnCancel {
                transaction.setResult("Canceled")
            }
            .doOnError { throwable ->
                transaction.captureException(throwable)
            }
            .doFinally {
                transaction.end()
            }
            .subscriberContext { context ->
                context.put(ApmContext.Key, ApmContext(transaction))
            }
    }
}
