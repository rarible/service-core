package com.rarible.core.loader.internal.common

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Clock

/**
 * Service responsible for pulling out tasks to be retried
 * and scheduling execution for them.
 *
 * All such tasks are stored in the database with [LoadTask.Status.WaitsForRetry]
 * and [LoadTask.Status.WaitsForRetry.rescheduled] value `false`.
 *
 * IMPLEMENTATION NOTE!
 * We firstly write the task message to Kafka and then update the status with `rescheduled = true` in the database.
 * It may happen that tasks are duplicated on the workers side if writing to database fails after writing to Kafka,
 * because after restart this service will try to re-schedule the same task again.
 * [LoadRunner] is responsible for handling duplicated tasks.
 *
 * We cannot have atomic write to Kafka and database, unfortunately.
 * Therefore, it is very probable that the worker will see `rescheduled = false` status when starting a task,
 * and then, while executing the task, this [RetryTasksService] will update the status to `rescheduled = true`
 * and update optimistic lock version of the [LoadTask.version]. In this case, on attempt to save an updated status,
 * the [LoadRunner] will fail with [OptimisticLockingFailureException].
 * [LoadRunner] will retry saving with the updated [LoadTask.version].
 */
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
                updateRetryTask(retryTask.copy(status = status.copy(rescheduled = true)))
            }
            .count()
        logger.info("Scheduled $numberOfTasks tasks for retry")
    }

    private suspend fun scheduleLoadTask(taskToRetry: LoadTask) {
        logger.info("Scheduling task to retry $taskToRetry")
        loadService.sendKafkaTask(taskToRetry.id, taskToRetry.type, true)
    }

    private suspend fun updateRetryTask(retryTask: LoadTask) {
        try {
            loadTaskService.save(retryTask)
        } catch (ex: OptimisticLockingFailureException) {
            logConcurrencyRace(retryTask)
        } catch (ex: DuplicateKeyException) {
            logConcurrencyRace(retryTask)
        }
    }

    private fun logConcurrencyRace(loadTask: LoadTask) {
        logger.warn(
            "Retry service has failed to save a new status for $loadTask, " +
                    "probably another worker has done that a little bit earlier. It is acceptable."
        )
    }

}
