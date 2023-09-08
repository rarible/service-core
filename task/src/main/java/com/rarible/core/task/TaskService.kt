package com.rarible.core.task

import io.micrometer.core.instrument.util.NamedThreadFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.concurrent.Executors

/**
 * Background service responsible for running and resuming [TaskHandler]s.
 */
@FlowPreview
@ExperimentalCoroutinesApi
@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val runner: TaskRunner,
    private val mongo: ReactiveMongoOperations,
    handlers: List<TaskHandler<*>>
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            NamedThreadFactory("task-")
        )
            .asCoroutineDispatcher()
    )

    val handlersMap = handlers.associateBy { it.type }

    init {
        scope.launch {
            taskRepository.findByRunning(true)
                .asFlow()
                .collect {
                    val handler = handlersMap[it.type]
                    if (handler != null) {
                        logger.info("marking as not running: ${it.type}:${it.param} $it")
                        taskRepository.save(it.clearRunning()).awaitFirst()
                    }
                }
        }
    }

    fun autorun() {
        handlersMap.values.forEach { handler ->
            handler.getAutorunParams().forEach { (param, sample) ->
                runTask(handler.type, param, sample)
            }
        }
    }

    fun runTasks() {
        scope.launch {
            logger.info("TaskHandler: find tasks to run")
            val tasksStarted = Flux.concat(
                taskRepository.findByRunningAndLastStatus(false, TaskStatus.ERROR),
                taskRepository.findByRunningAndLastStatus(false, TaskStatus.NONE)
            )
                .asFlow()
                .map {
                    runTask(it.type, it.param, it.sample)
                    logger.info("TaskHandler: started task $it")
                }
                .count()
            logger.info("TaskHandler: started $tasksStarted tasks")
        }
    }

    fun runTask(type: String, param: String, sample: Long? = Task.DEFAULT_SAMPLE) {
        scope.launch {
            val handler = handlersMap[type]
            if (handler != null) {
                logger.info("runTask type=$type param=$param")
                runner.runLongTask(param, handler, sample)
            } else {
                logger.info("TaskHandler $type not found. skipping")
            }
        }
    }

    fun findTasks(type: String, param: String? = null): Flow<Task> {
        val typeCriteria = Task::type isEqualTo type
        val c = param?.run {
            Criteria().andOperator(
                typeCriteria,
                Task::param isEqualTo this
            )
        } ?: typeCriteria
        return mongo.find<Task>(Query(c)).asFlow()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TaskService::class.java)
    }
}
