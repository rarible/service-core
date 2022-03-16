package com.rarible.core.loader.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.util.concurrent.TimeUnit

const val LOADER_PROPERTIES_PREFIX = "rarible.loader"

/**
 * Configuration properties of the loader infrastructure.
 *
 * By default, the current application can both schedule loading tasks for execution and be the worker to execute them.
 * To disable the worker functionality, set `rarible.loader.enableWorkers = false` in the application yaml.
 */
@ConfigurationProperties(LOADER_PROPERTIES_PREFIX)
@ConstructorBinding
data class LoadProperties(
    /**
     * Kafka cluster where loading tasks and notifications are sent to and read from.
     */
    val brokerReplicaSet: String,
    /**
     * The prefix used for naming loader infrastructure topics.
     *
     * For example, if the [topicsPrefix] is equal to `my-service`,
     * the loader will use topics `my-service.loader-tasks-<type>` and `my-service.loader-notifications-<type>`
     * for storing tasks and notifications.
     */
    val topicsPrefix: String,
    /**
     * The number of partitions to use for the tasks topics. May be used to tune performance.
     *
     * NOTE! TODO: for now, this option should be set in all services' configurations. Topics are created by the first started service.
     */
    val loadTasksTopicPartitions: Int = 10,
    /**
     * The number of instances of the application that performs the loading.
     * Every instance will process tasks from [loadTasksTopicPartitions] / [loadTasksServiceInstances] partitions.
     * By default, this is 1, so that there is only one application loading the tasks.
     */
    val loadTasksServiceInstances: Int = 1,
    /**
     * The number of partitions to use for loading notifications' topics. May be used to tune performance.
     *
     * NOTE! TODO: for now, this option should be set in all services' configurations. Topics are created by the first started service.
     */
    val loadNotificationsTopicPartitions: Int = 10,
    /**
     * The number of notifications pulled out in batch for processing in parallel by notification listeners.
     */
    val loadNotificationsBatchSize: Int = 50,
    /**
     * The number of tasks pulled out in batch for processing in parallel using [workers] threads.
     */
    val loadTasksBatchSize: Int = 50,
    /**
     * The number of worker threads that execute the loading tasks.
     *
     * This field is only applicable if the [enableWorkers] is true.
     */
    val workers: Int = 4,
    /**
     * The number of worker threads that call the notification listeners.
     *
     * This field is only applicable if the [enableNotifications] is true.
     */
    val notificationListenersCallerWorkers: Int = 4,
    /**
     * Retry policy for failed tasks.
     */
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
