package com.rarible.core.loader.internal

import com.rarible.core.loader.LoadService
import com.rarible.core.loader.LoadTaskStatus
import com.rarible.core.loader.LoadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class LoadServiceImpl(
    private val loadTaskKafkaSender: LoadTaskKafkaSender,
    private val loadTaskService: LoadTaskService,
    private val clock: Clock,
    private val loadMetrics: LoadMetrics
) : LoadService {

    private val logger = LoggerFactory.getLogger(LoadService::class.java)

    override suspend fun scheduleLoad(
        loadType: LoadType,
        key: String
    ): LoadTaskId {
        val loadTaskId = generateLoadTaskId()
        val scheduledAt = clock.instant()
        val status = LoadTask.Status.Scheduled(scheduledAt)
        val loadTask = LoadTask(
            id = loadTaskId,
            type = loadType,
            key = key,
            status = status
        )
        loadTaskService.save(loadTask)
        logger.info("Scheduling task to run $loadTask")
        sendKafkaTask(loadTask.id, loadTask.type, false)
        return loadTaskId
    }

    override suspend fun getStatus(taskId: LoadTaskId): LoadTaskStatus? =
        loadTaskService.get(taskId)?.status?.toApiStatus()

    // Used from retry scheduler only, not public API.
    internal suspend fun sendKafkaTask(
        loadTaskId: LoadTaskId,
        loadType: LoadType,
        forRetry: Boolean
    ) {
        loadTaskKafkaSender.send(loadTaskId, loadType)
        loadMetrics.onTaskScheduled(loadType, forRetry)
    }
}
