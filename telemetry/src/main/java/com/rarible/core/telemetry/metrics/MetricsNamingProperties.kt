package com.rarible.core.telemetry.metrics

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("management.metrics.naming")
class MetricsNamingProperties {
    val suffixTags = mutableListOf<String>()
}
