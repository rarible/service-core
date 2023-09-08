package com.rarible.core.apm.web.filter.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val RARIBLE_FILTER_APM = "rarible.core.apm.web.filter"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_FILTER_APM)
data class ApmFilterProperties(
    val enabled: Boolean = true
)
