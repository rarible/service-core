package com.rarible.core.loader.internal

import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.Loader
import com.rarible.core.loader.configuration.LoadProperties
import org.slf4j.LoggerFactory
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
    private val loaderCriticalCodeExecutor: LoaderCriticalCodeExecutor
) {
    private val logger = LoggerFactory.getLogger(LoadRunner::class.java)

    init {
        loaders.map { it.type }.groupingBy { it }.eachCount().forEach { (key, count) ->
            check(count == 1) { "Loader $key is duplicated" }
        }
    }

    private val loaders = loaders.associateBy { it.type }

    suspend fun load(taskId: LoadTaskId) {
        val loadTask = getLoadTaskWithDeduplicationCheck(taskId) ?: return
        val loader = loaders[loadTask.type]
        if (loader == null) {
            logger.warn("No loader found for ${loadTask.type}, skipping task $loadTask")
            return
        }
        logger.info("Starting loading $loadTask")
        try {
            loader.load(loadTask.key)
        } catch (e: Throwable) {
            if (e is Error) {
                logger.error("Fatal failure to load $loadTask", e)
                throw e
            }
            logger.info("Failed to load $loadTask", e)
            val errorMessage = e.localizedMessage ?: e.message ?: e::class.java.simpleName
            val failedAt = clock.nowMillis()
            val failedTask = loaderCriticalCodeExecutor.retryOrFatal("Schedule retry of $loadTask") {
                updateFailedTask(loadTask, failedAt, errorMessage)
            }
            loaderCriticalCodeExecutor.retryOrFatal("Send failure notification for $loadTask") {
                loadNotificationKafkaSender.send(
                    LoadNotification(
                        taskId = taskId,
                        type = loadTask.type,
                        key = loadTask.key,
                        status = failedTask.status.toApiStatus()
                    )
                )
            }
            return
        }
        val loadedAt = clock.instant()
        logger.info("Loaded successfully $loadTask")
        val loadedTask = loaderCriticalCodeExecutor.retryOrFatal("Update loaded task $loadTask") {
            updateLoadedTask(loadTask, loadedAt)
        }
        loaderCriticalCodeExecutor.retryOrFatal("Send Loaded notification for $loadedTask") {
            loadNotificationKafkaSender.send(
                LoadNotification(
                    taskId = taskId,
                    type = loadedTask.type,
                    key = loadedTask.key,
                    status = loadedTask.status.toApiStatus()
                )
            )
        }
    }

    private suspend fun getLoadTaskWithDeduplicationCheck(taskId: LoadTaskId): LoadTask? {
        val loadTask = loadTaskService.get(taskId)
        if (loadTask == null) {
            // Generally, must not happen, an error in the DB/Kafka synchronization.
            logger.error("No task found for $taskId")
            return null
        }
        /*
        Perform Kafka messages de-duplication. Some task may be received from Kafka multiple times
        even though it has already been completed. Just skip such tasks.
         */
        return when (loadTask.status) {
            is LoadTask.Status.Loaded -> {
                logger.warn("Attempted to load the already loaded task $loadTask")
                return null
            }
            is LoadTask.Status.Failed -> {
                logger.warn("Attempted to load the already failed task $loadTask")
                return null
            }
            is LoadTask.Status.Scheduled -> loadTask
            is LoadTask.Status.WaitsForRetry -> loadTask
        }
    }

    private suspend fun updateLoadedTask(
        loadTask: LoadTask,
        loadedAt: Instant
    ): LoadTask {
        val newStatus = getNewLoadedStatus(loadTask, loadedAt)
        val newLoadTask = loadTask.copy(status = newStatus)
        logger.info("Updated loaded task status $newLoadTask")
        // TODO[loader]: handle exception on saving.
        return loadTaskService.save(newLoadTask)
    }

    private suspend fun updateFailedTask(
        loadTask: LoadTask,
        failedAt: Instant,
        errorMessage: String
    ): LoadTask {
        val newStatus = getNewFailedStatus(loadTask, failedAt, errorMessage)
        val newLoadTask = loadTask.copy(status = newStatus)
        logger.info("Updated failed task $newLoadTask")
        // TODO[loader]: handle exception on saving.
        return loadTaskService.save(newLoadTask)
    }

    private fun getNewLoadedStatus(
        loadTask: LoadTask,
        loadedAt: Instant
    ): LoadTask.Status = when (val status = loadTask.status) {
        is LoadTask.Status.Failed, is LoadTask.Status.Loaded ->
            // TODO[loader]: add a test for this case.
            throw AssertionError("Task $loadTask must have been ignored by the LoadRunner")
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
        is LoadTask.Status.Failed, is LoadTask.Status.Loaded -> status // Generally, should not happen (filtered out earlier).
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
                retryAttempts = status.retryAttempts + 1,
                failedAt = failedAt,
                errorMessage = errorMessage
            )
        }
    }
}
