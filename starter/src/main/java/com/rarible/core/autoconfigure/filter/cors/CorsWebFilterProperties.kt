package com.rarible.core.autoconfigure.filter.cors

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val RARIBLE_FILTER_CORS = "rarible.core.filter.cors"

@ConfigurationProperties(RARIBLE_FILTER_CORS)
@ConstructorBinding
data class CorsWebFilterProperties(
    val enabled: Boolean = false
)
