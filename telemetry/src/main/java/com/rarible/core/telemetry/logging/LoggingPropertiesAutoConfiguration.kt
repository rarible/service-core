package com.rarible.core.telemetry.logging

import org.springframework.boot.context.properties.EnableConfigurationProperties

@EnableConfigurationProperties(
    LogstashTcpSocketLoggingProperties::class
)
internal class LoggingPropertiesAutoConfiguration
