package com.rarible.core.loader.internal

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class RetryTasksService(
    private val loadService: LoadServiceImpl,
    private val loadTaskService: LoadTaskService,
    private val clock: Clock
) {

    private val logger = LoggerFactory.getLogger(RetryTasksService::class.java)

    suspend fun scheduleTasksToRetry() {
        logger.info("Searching for the ready to retry tasks")
        val retryTasks = loadTaskService.getTasksToRetry(now = clock.instant())
        val numberOfTasks = retryTasks
            .onEach { retryTask ->
                val status = retryTask.status
                check(status is LoadTask.Status.WaitsForRetry)
                scheduleLoadTask(retryTask)
                updateRetryTask(retryTask, status)
            }
            .count()
        logger.info("Scheduled $numberOfTasks tasks for retry")
    }

    private suspend fun scheduleLoadTask(taskToRetry: LoadTask) {
        logger.info("Scheduling task to retry $taskToRetry")
        loadService.scheduleLoad(taskToRetry)
    }

    private suspend fun updateRetryTask(loadTask: LoadTask, status: LoadTask.Status.WaitsForRetry) {
        try {
            loadTaskService.save(loadTask.copy(status = status.copy(rescheduled = true)))
        } catch (ex: OptimisticLockingFailureException) {
            logConcurrencyRace(loadTask)
        } catch (ex: DuplicateKeyException) {
            logConcurrencyRace(loadTask)
        }
    }

    private fun logConcurrencyRace(loadTask: LoadTask) {
        logger.warn(
            "Retry service has failed to save a new status for $loadTask, " +
                    "probably another worker has done that a little bit earlier. It is acceptable."
        )
    }

}
