package com.rarible.core.loader

import com.rarible.core.loader.internal.LoadTaskId

/**
 * Entrypoint API to schedule background loaders for execution at some point in the future.
 *
 * We use Kafka queue of tasks for storing scheduled tasks,
 * so restarts and failures of the workers are handled properly.
 *
 * The tasks are guaranteed to be executed at least once, BUT MAY BE EXECUTED SEVERAL times,
 * if an unlucky coincidence of failures/restarts of workers happen.
 *
 * Failed tasks are scheduled for retry with a retry policy configured via `LoadProperties.
 *
 * Clients wishing to receive notifications about completed tasks must
 * register [LoadNotificationListener]-s `@Component` beans.
 */
interface LoadService {
    /**
     * Schedule execution of a task, see the doc of [LoadService].
     */
    suspend fun scheduleLoad(loadType: LoadType, key: String): LoadTaskId

    /**
     * Get the current [status][LoadTaskStatus] of the task having ID [taskId]
     * or `null` if the task is unknown.
     */
    suspend fun getStatus(taskId: LoadTaskId): LoadTaskStatus?
}
