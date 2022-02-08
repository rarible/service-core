package com.rarible.core.loader.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Configuration properties of the loader infrastructure.
 * - [brokerReplicaSet] - the Kafka cluster's address.
 * - [loadTasksTopicPartitions] and [loadNotificationsTopicPartitions] - configure
 * the internal Kafka queues used by workers and notifiers, these options affect performance.
 * - [workers] - the number of worker threads that execute the loading tasks
 * - [retry] - retry policy
 */
@ConfigurationProperties("rarible.loader")
@ConstructorBinding
data class LoadProperties(
    val brokerReplicaSet: String,
    val topicsPrefix: String = "loader", // TODO: remove the default value after all clients specify it.
    val loadTasksTopicPartitions: Int = 10,
    val loadNotificationsTopicPartitions: Int = 10,
    val workers: Int = 1,
    val retry: RetryProperties = RetryProperties()
)

/**
 * Retry policy applied when tasks fail.
 * Tasks will be retried after increasing delays configured by [backoffDelaysMillis].
 */
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
