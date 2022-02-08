package com.rarible.core.loader.internal.runner

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RetryTasksSchedulerSpringJob(
    private val retryTasksService: RetryTasksService
) {

    private val logger = LoggerFactory.getLogger(RetryTasksSchedulerSpringJob::class.java)

    @Scheduled(
        // Use the same property for the delays.
        initialDelayString = "\${rarible.loader.retry.job.runner.period:30000}",
        fixedDelayString = "\${rarible.loader.retry.job.runner.period:30000}"
    )
    fun scheduledRetryTasks() {
        logger.info("Scheduling tasks to retry")
        try {
            runBlocking { retryTasksService.scheduleTasksToRetry() }
        } catch (e: Throwable) {
            logger.error("Failed to schedule tasks to retry", e)
            throw e
        }
        logger.info("Finished scheduling tasks to retry")
    }
}
