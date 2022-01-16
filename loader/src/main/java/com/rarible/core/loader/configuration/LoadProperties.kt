package com.rarible.core.loader.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.util.concurrent.TimeUnit

@ConfigurationProperties("rarible.loader")
@ConstructorBinding
data class LoadProperties(
    val brokerReplicaSet: String,
    val loadTasksTopicPartitions: Int = 10,
    val loadNotificationsTopicPartitions: Int = 10,
    val workers: Int = 1,
    val retry: RetryProperties = RetryProperties()
)

data class RetryProperties(
    val backoffDelaysMillis: List<Long> = listOf(
        TimeUnit.MINUTES.toMillis(1),
        TimeUnit.MINUTES.toMillis(15),
        TimeUnit.HOURS.toMillis(1),
        TimeUnit.HOURS.toMillis(6),
        TimeUnit.HOURS.toMillis(24)
    )
) {
    val retryAttempts: Int get() = backoffDelaysMillis.size

    fun getRetryDelay(attempt: Int): Duration? =
        backoffDelaysMillis.getOrNull(attempt)?.let { Duration.ofMillis(it) }
}
