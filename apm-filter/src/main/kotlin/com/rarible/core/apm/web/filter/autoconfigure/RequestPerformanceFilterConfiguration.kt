package com.rarible.core.apm.web.filter.autoconfigure

import com.rarible.core.apm.web.filter.RequestPerformanceFilter
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@ConditionalOnProperty(
    prefix = RARIBLE_FILTER_APM,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(ApmFilterProperties::class)
class RequestPerformanceFilterConfiguration(
    private val environmentInfo: ApplicationEnvironmentInfo,
    private val applicationInfo: ApplicationInfo
) {
    @Bean
    fun requestPerformanceFilter(): RequestPerformanceFilter {
        return RequestPerformanceFilter(environmentInfo, applicationInfo)
    }
}
