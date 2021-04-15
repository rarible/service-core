package com.rarible.core.task

import com.rarible.core.common.optimisticLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

//todo log with context
@Service
class TaskRunner(
    private val eventPublisher: ApplicationEventPublisher,
    private val taskRepository: TaskRepository
) {
    @ExperimentalCoroutinesApi
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> runLongTask(param: String, handler: TaskHandler<T>) {
        logger.info("running ${handler.type} with param=$param")
        val task = findAndMarkRunning(handler.isAbleToRun(param), handler.type, param)
        if (task != null) {
            runAndSaveTask(task, handler)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> runAndSaveTask(task: Task, handler: TaskHandler<T>) {
        eventPublisher.publishEvent(TaskRunnerEvent.TaskStartEvent(task)) //todo make sure this is async
        logger.info("starting task $task")
        var current = task
        try {
            handler.runLongTask(task.state as T?, task.param)
                .collect { next ->
                    logger.info("new task state for ${handler.type} with param=${task.param}: $next")
                    current = taskRepository.save(current.withState(next)).awaitFirst()
                }
            logger.info("completed ${handler.type} with param=${task.param}")
            val saved = taskRepository.save(current.markCompleted()).awaitFirst()
            eventPublisher.publishEvent(TaskRunnerEvent.TaskCompleteEvent(saved))
        } catch (e: Throwable) {
            logger.info("error caught executing ${handler.type} with param=${task.param}", e)
            val saved = taskRepository.save(current.markError(e)).awaitFirst()
            eventPublisher.publishEvent(TaskRunnerEvent.TaskErrorEvent(saved))
        }
    }

    private suspend fun findAndMarkRunning(canRun: Boolean, type: String, param: String): Task? {
        return optimisticLock {
            val task = taskRepository.findByTypeAndParam(type, param).awaitFirstOrNull()
            if (task == null) {
                val newTask = Task(
                    type = type,
                    param = param,
                    lastStatus = TaskStatus.NONE,
                    state = null,
                    running = canRun
                )
                taskRepository.save(newTask).awaitFirst()
                    .let { if (it.running) it else null }
            } else if (canRun && !task.running && task.lastStatus != TaskStatus.COMPLETED) {
                taskRepository.save(task.markRunning()).awaitFirst()
            } else {
                null
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TaskRunner::class.java)
    }
}