package com.rarible.core.mongo.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("rarible.core.mongodb")
data class MongoProperties(
    val maxConnectionLifeTime: Duration? = null,
    val maxConnectionIdleTime: Duration? = null,
    val maxTime: Duration? = null,
    val enableMetrics: Boolean = false,
)
