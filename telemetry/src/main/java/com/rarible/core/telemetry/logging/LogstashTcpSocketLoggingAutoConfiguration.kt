package com.rarible.core.telemetry.logging

import ch.qos.logback.classic.filter.ThresholdFilter
import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import com.rarible.core.telemetry.environment.EnvironmentAutoConfiguration
import net.logstash.logback.appender.LogstashTcpSocketAppender
import net.logstash.logback.encoder.LogstashEncoder
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered

@ConditionalOnProperty("logging.logstash.tcp-socket.enabled")
@AutoConfigureAfter(EnvironmentAutoConfiguration::class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 1)
internal class LogstashTcpSocketLoggingAutoConfiguration(
    private val properties: LogstashTcpSocketLoggingProperties,
    private val environmentInfo: ApplicationEnvironmentInfo,
    private val applicationInfo: ApplicationInfo
) {
    init {
        val encoder = logstashTcpSocketEncoder()
        val appender = logstashTcpSocketAppender(encoder)

        val loggerContext = getLogbackLoggerContext()
        val rootLogger = loggerContext.getRootLogger()
        rootLogger.addAppender(appender)
    }

    private fun logstashTcpSocketAppender(logstashEncoder: LogstashEncoder): LogstashTcpSocketAppender {
        val loggerContext = getLogbackLoggerContext()

        val levelFilter = ThresholdFilter().apply {
            setLevel(properties.level)
            start()
        }
        return LogstashTcpSocketAppender().apply {
            name = "logstash"
            context = loggerContext
            addDestination(properties.destinations.joinToString { "${it.host}:${it.port}" })
            addFilter(levelFilter)
            encoder = logstashEncoder
            start()
        }
    }

    private fun logstashTcpSocketEncoder(): LogstashEncoder {
        val loggerContext = getLogbackLoggerContext()

        val globalCustomFields = ObjectMapper().writeValueAsString(
            mapOf(
                "service" to applicationInfo.serviceName,
                "environment" to environmentInfo.name,
                "host" to environmentInfo.host
            )
        )
        return LogstashEncoder().apply {
            context = loggerContext
            customFields = globalCustomFields
            start()
        }
    }
}
