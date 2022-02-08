package com.rarible.core.loader

import com.rarible.core.loader.internal.common.LoadTaskId

/**
 * Entrypoint API to schedule background loaders for execution in background by worker services.
 * Workers may reside in the same application, see `LoadProperties.enableWorkers`.
 *
 * We use Kafka queue of tasks for storing scheduled tasks,
 * so restarts and failures of the workers are handled properly.
 *
 * The tasks are guaranteed to be executed at least once, BUT may be executed several times,
 * if an unlucky coincidence of failures/restarts of workers happen.
 * Tasks de-duplication may be considered later.
 *
 * Failed tasks are scheduled for retry with a configured retry policy (see `LoadProperties`).
 *
 * Clients that want to receive notifications about tasks lifecycle can
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
