package com.rarible.core.loader

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
     * Schedule execution of a task with a newly [generated][generateLoadTaskId] task ID.
     * Returns the task ID to [track][getStatus] the task status.
     */
    suspend fun scheduleLoad(loadType: LoadType, key: String): LoadTaskId =
        scheduleLoad(loadType, key, generateLoadTaskId())

    /**
     * Schedule execution of a task with the specified task ID, see the doc of [LoadService].
     * Returns the given [loadTaskId].
     * Note that [loadTaskId] must be a unique task ID, otherwise a [IllegalArgumentException] is thrown.
     */
    suspend fun scheduleLoad(
        loadType: LoadType,
        key: String,
        loadTaskId: LoadTaskId
    ): LoadTaskId

    /**
     * Get the current [status][LoadTaskStatus] of the task having ID [taskId]
     * or `null` if the task is unknown.
     */
    suspend fun getStatus(taskId: LoadTaskId): LoadTaskStatus?
}
