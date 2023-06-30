package com.rarible.core.task

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

internal const val RARIBLE_TASK = "rarible.core.task"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_TASK)
data class RaribleTaskProperties(
    val enabled: Boolean = true,
    val initialDelay: Duration = Duration.ofSeconds(30),
    val pollingPeriod: Duration = Duration.ofSeconds(60),
    val errorDelay: Duration = Duration.ofSeconds(60),
)