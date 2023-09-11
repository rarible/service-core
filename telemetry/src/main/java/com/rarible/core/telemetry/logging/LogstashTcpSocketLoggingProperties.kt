package com.rarible.core.telemetry.logging

import net.logstash.logback.appender.AsyncDisruptorAppender
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import java.net.URI
import javax.validation.constraints.NotEmpty

@ConfigurationProperties("logging.logstash.tcp-socket")
@ConditionalOnProperty("logging.logstash.tcp-socket.enabled")
@ConstructorBinding
@Validated
internal data class LogstashTcpSocketLoggingProperties(
    var enabled: Boolean = true,
    /**
     * Minimum level of messages to be logged.
     */
    var level: String = "INFO",
    /**
     * List of destinations to send logs.
     */
    @NotEmpty
    var destinations: List<URI>,

    var ringBufferSize: Int = AsyncDisruptorAppender.DEFAULT_RING_BUFFER_SIZE * 4
)
