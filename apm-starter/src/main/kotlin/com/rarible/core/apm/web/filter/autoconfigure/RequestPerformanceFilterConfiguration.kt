package com.rarible.core.apm.web.filter.autoconfigure

import com.rarible.core.apm.web.filter.RequestPerformanceFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping

@ConditionalOnProperty(
    prefix = RARIBLE_FILTER_APM,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(ApmFilterProperties::class)
class RequestPerformanceFilterConfiguration(
    private val handler: DispatcherHandler
) {
    @Bean
    fun handlerMapping(): HandlerMapping {
        return RequestMappingHandlerMapping()
    }

    @Bean
    fun requestPerformanceFilter(): RequestPerformanceFilter {
        return RequestPerformanceFilter(handler)
    }
}
