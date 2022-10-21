package com.rarible.core.loader.internal.common

import com.rarible.core.loader.LoadTaskId
import kotlinx.coroutines.flow.Flow
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Instant

interface LoadTaskService {
    fun getAll(): Flow<LoadTask>
    suspend fun get(id: LoadTaskId): LoadTask?
    fun getTasksToRetry(now: Instant): Flow<LoadTask>
    @Throws(OptimisticLockingFailureException::class)
    suspend fun save(task: LoadTask): LoadTask
    suspend fun remove(task: LoadTask)
}

@Component
class LoadTaskServiceImpl(
    private val loadTaskRepository: LoadTaskRepository,
    private val updateListeners: List<LoadTaskUpdateListener>
) : LoadTaskService {

    override fun getAll(): Flow<LoadTask> =
        loadTaskRepository.getAll()

    override suspend fun get(id: LoadTaskId): LoadTask? =
        loadTaskRepository.get(id)

    override fun getTasksToRetry(now: Instant): Flow<LoadTask> =
        loadTaskRepository.getTasksToRetry(now)

    override suspend fun save(task: LoadTask): LoadTask {
        val result = loadTaskRepository.save(task)
        updateListeners.forEach { it.onTaskSaved(result) }
        return result
    }

    override suspend fun remove(task: LoadTask) =
        loadTaskRepository.remove(task)
}
