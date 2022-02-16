package com.rarible.core.loader.internal.runner

import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.Loader
import com.rarible.core.loader.configuration.LoadProperties
import com.rarible.core.loader.internal.common.LoadMetrics
import com.rarible.core.loader.internal.common.LoadNotificationKafkaSender
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.loader.LoadTaskId
import com.rarible.core.loader.internal.common.LoadTaskService
import com.rarible.core.loader.internal.common.nowMillis
import com.rarible.core.loader.internal.common.toApiStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class LoadRunner(
    loaders: List<Loader>,
    private val loadProperties: LoadProperties,
    private val loadNotificationKafkaSender: LoadNotificationKafkaSender,
    private val loadTaskService: LoadTaskService,
    private val clock: Clock,
    private val loadCriticalCodeExecutor: LoadCriticalCodeExecutor,
    private val loadMetrics: LoadMetrics
) {
    private val logger = LoggerFactory.getLogger(LoadRunner::class.java)

    init {
        loaders.map { it.type }.groupingBy { it }.eachCount().forEach { (key, count) ->
            check(count == 1) { "Loader $key is duplicated" }
        }
    }

    private val loaders = loaders.associateBy { it.type }

    suspend fun load(taskId: LoadTaskId) {
        val loadTask = getLoadTask(taskId) ?: return
        val loader = loaders[loadTask.type]
        if (loader == null) {
            logger.warn("No loader found for ${loadTask.type}, skipping task $loadTask")
            return
        }
        logger.info("Starting loading $loadTask")
        val loaderSample = loadMetrics.onLoaderStarted()
        val newStatus = try {
            loader.load(loadTask.key)
            val loadedAt = clock.instant()
            logger.info("Loaded successfully $loadTask")
            loadMetrics.onLoaderSuccess(loadTask.type, loaderSample)
            getNewLoadedStatus(loadTask, loadedAt)
        } catch (e: Throwable) {
            loadMetrics.onLoaderFailed(loadTask.type, loaderSample)
            if (e is Error) {
                logger.error("Fatal failure to load $loadTask", e)
                throw e
            }
            logger.info("Failed to load $loadTask", e)
            val errorMessage = e.localizedMessage ?: e.message ?: e::class.java.simpleName
            val failedAt = clock.nowMillis()
            getNewFailedStatus(loadTask, failedAt, errorMessage)
        }
        logger.info("Updating task status of task $loadTask to $newStatus")

        val newLoadTask = saveNewTaskOrTerminate(loadTask, newStatus)
            ?: return // Terminate, if we have failed to update the status.

        loadCriticalCodeExecutor.retryOrFatal("Send notification for $newLoadTask") {
            loadNotificationKafkaSender.send(
                LoadNotification(
                    taskId = taskId,
                    type = newLoadTask.type,
                    key = newLoadTask.key,
                    status = newLoadTask.status.toApiStatus()
                )
            )
        }
    }

    private suspend fun saveNewTaskOrTerminate(loadTask: LoadTask, newStatus: LoadTask.Status): LoadTask? {
        try {
            return loadTaskService.save(loadTask.copy(status = newStatus))
        } catch (e: OptimisticLockingFailureException) {
            /**
            If we fail to update the task status it can happen only in two cases:
            1) Another worker has executed this task earlier and updated the status.
            In theory, this can only happen because of Kafka messages duplication, and probability is very low.

            2) [RetryTasksService] firstly writes a retryable task (with [LoadTask.Status.WaitsForRetry.rescheduled] value `false`)
            and then updates the `rescheduled` to `true`. In this case the [LoadTask.version] will be updated.
            We should simply retry saving the task here.
             */
            val actualTask = loadTaskService.get(loadTask.id)
            if (actualTask == null) {
                // Generally, should never happen.
                logger.error("Task disappeared $loadTask")
                return null
            }
            if (loadTask.status is LoadTask.Status.WaitsForRetry
                && actualTask.status is LoadTask.Status.WaitsForRetry
                && actualTask.status == loadTask.status.copy(rescheduled = true)
            ) {
                try {
                    // On success, resume to saving the notification.
                    return loadTaskService.save(
                        loadTask.copy(
                            version = actualTask.version,
                            status = newStatus
                        )
                    )
                } catch (oe: OptimisticLockingFailureException) {
                    // Apparently, another worker has updated the task again.
                }
            }
            logger.info("Another worker has updated the task concurrently, old task $loadTask, new task $actualTask")
            return null
        }
    }

    private suspend fun getLoadTask(taskId: LoadTaskId): LoadTask? {
        val loadTask = loadTaskService.get(taskId)
        if (loadTask == null) {
            // Generally, must not happen, an error in the DB/Kafka synchronization.
            logger.error("No task found for $taskId")
            return null
        }
        /*
        Perform Kafka messages de-duplication. Some tasks may be received from Kafka multiple times
        even though they have already completed. Just skip them.
         */
        return when (loadTask.status) {
            is LoadTask.Status.Loaded -> {
                logger.info("Attempted to load the already loaded task $loadTask")
                return null
            }
            is LoadTask.Status.Failed -> {
                logger.info("Attempted to load the already failed task $loadTask")
                return null
            }
            is LoadTask.Status.Scheduled -> loadTask
            is LoadTask.Status.WaitsForRetry -> loadTask
        }
    }

    private fun getNewLoadedStatus(
        loadTask: LoadTask,
        loadedAt: Instant
    ): LoadTask.Status = when (val status = loadTask.status) {
        is LoadTask.Status.Failed, is LoadTask.Status.Loaded -> status // These will not happen here, because they were ignored.
        is LoadTask.Status.Scheduled -> LoadTask.Status.Loaded(
            scheduledAt = status.scheduledAt,
            retryAttempts = status.retryAttempts,
            loadedAt = loadedAt
        )
        is LoadTask.Status.WaitsForRetry -> LoadTask.Status.Loaded(
            scheduledAt = status.scheduledAt,
            retryAttempts = status.retryAttempts,
            loadedAt = loadedAt
        )
    }

    private fun getNewFailedStatus(
        loadTask: LoadTask,
        failedAt: Instant,
        errorMessage: String
    ): LoadTask.Status = when (val status = loadTask.status) {
        is LoadTask.Status.Failed, is LoadTask.Status.Loaded -> status // These will not happen here, because they were ignored.
        is LoadTask.Status.Scheduled -> if (loadProperties.retry.retryAttempts > 0) {
            LoadTask.Status.WaitsForRetry(
                scheduledAt = loadTask.status.scheduledAt,
                retryAttempts = 0,
                retryAt = failedAt + loadProperties.retry.getRetryDelay(0),
                failedAt = failedAt,
                errorMessage = errorMessage,
                rescheduled = false
            )
        } else {
            LoadTask.Status.Failed(
                scheduledAt = loadTask.status.scheduledAt,
                retryAttempts = 0,
                failedAt = failedAt,
                errorMessage = errorMessage
            )
        }
        is LoadTask.Status.WaitsForRetry -> if (status.retryAttempts < loadProperties.retry.retryAttempts - 1) {
            LoadTask.Status.WaitsForRetry(
                scheduledAt = loadTask.status.scheduledAt,
                retryAttempts = status.retryAttempts + 1,
                retryAt = failedAt + loadProperties.retry.getRetryDelay(status.retryAttempts + 1),
                failedAt = failedAt,
                errorMessage = errorMessage,
                rescheduled = false
            )
        } else {
            LoadTask.Status.Failed(
                scheduledAt = loadTask.status.scheduledAt,
                retryAttempts = status.retryAttempts,
                failedAt = failedAt,
                errorMessage = errorMessage
            )
        }
    }
}
