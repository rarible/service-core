package com.rarible.core.loader.internal

import com.rarible.core.loader.LoadNotification
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
    private val loadNotificationKafkaSender: LoadNotificationKafkaSender
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
        // TODO[loader]: handle restarts and errors: if the task is written in the DB but not in Kafka, it is a problem.
        loadTaskService.save(loadTask)
        scheduleLoad(loadTask)
        loadNotificationKafkaSender.send(
            LoadNotification(
                taskId = loadTaskId,
                type = loadType,
                key = key,
                status = status.toApiStatus()
            )
        )
        return loadTaskId
    }

    override suspend fun getStatus(taskId: LoadTaskId): LoadTaskStatus? =
        loadTaskService.get(taskId)?.status?.toApiStatus()

    // Used from retry scheduler only, not public API.
    internal suspend fun scheduleLoad(loadTask: LoadTask) {
        // TODO[loader]: review logs' messages, improve toString().
        logger.info("Scheduling task to run $loadTask")
        loadTaskKafkaSender.send(loadTask)
    }
}
