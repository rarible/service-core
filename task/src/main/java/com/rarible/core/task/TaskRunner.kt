package com.rarible.core.task

import com.rarible.core.common.optimisticLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * NOT A PUBLIC API.
 *
 * Internal task runner responsible for running tasks, handling errors and saving tasks' states.
 */
@FlowPreview
@Service
class TaskRunner(
    private val taskRepository: TaskRepository
) {
    @ExperimentalCoroutinesApi
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> runLongTask(param: String, handler: TaskHandler<T>, sample: Long? = Task.DEFAULT_SAMPLE) {
        val canRun = handler.isAbleToRun(param)
        val task = findAndMarkRunning(canRun, handler.type, param, sample)
        if (task != null) {
            logger.info("running ${handler.type} with param=$param")
            runAndSaveTask(task, handler)
        } else if (!canRun) {
            logger.info("task is not ready to run ${handler.type} with param=$param")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> runAndSaveTask(task: Task, handler: TaskHandler<T>) {
        logger.info("starting task $task")
        var current = task
        try {
            handler.runLongTask(task.state as T?, task.param)
                .let { if (task.sample?.takeIf { sample -> sample > 0 } != null) it.sample(task.sample) else it }
                .collect { next ->
                    logger.info("new task state for ${handler.type} with param=${task.param}: $next")
                    current = taskRepository.save(current.withState(next)).awaitFirst()
                }
            logger.info("completed ${handler.type} with param=${task.param}")
            taskRepository.save(current.markCompleted()).awaitFirst()
        } catch (e: Throwable) {
            logger.info("error caught executing ${handler.type} with param=${task.param}", e)
            taskRepository.save(current.markError(e)).awaitFirst()
        }
    }

    private suspend fun findAndMarkRunning(canRun: Boolean, type: String, param: String, sample: Long?): Task? {
        return optimisticLock {
            val task = taskRepository.findByTypeAndParam(type, param).awaitFirstOrNull()
            if (task == null) {
                val newTask = Task(
                    type = type,
                    param = param,
                    lastStatus = TaskStatus.NONE,
                    state = null,
                    running = canRun,
                    sample = sample
                )
                taskRepository.save(newTask).awaitFirst()
                    .let { if (it.running) it else null }
            } else if (canRun && !task.running && task.lastStatus != TaskStatus.COMPLETED) {
                taskRepository.save(task.markRunning().withSample(sample)).awaitFirst()
            } else {
                null
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TaskRunner::class.java)
    }
}
