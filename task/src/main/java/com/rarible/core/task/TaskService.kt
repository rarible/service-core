package com.rarible.core.task

import com.rarible.core.common.mapAsync
import com.rarible.core.common.nowMillis
import io.micrometer.core.instrument.util.NamedThreadFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

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
    handlers: List<TaskHandler<*>>,
    raribleTaskProperties: RaribleTaskProperties,
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            NamedThreadFactory("task-")
        )
            .asCoroutineDispatcher()
    )

    val handlersMap = handlers.associateBy { it.type }
    val runningTasksCount = AtomicInteger(0)
    val concurrency = raribleTaskProperties.concurrency

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

    suspend fun runStreamingTaskPoller(
        pollingDelay: Duration,
        delayFunction: suspend (Duration) -> Unit = { delay(it) }
    ) {
        val pendingTasksFlow = flow {
            while (coroutineContext.isActive) {
                logger.info("TaskHandler: flow of next tasks to run")
                val tasksToRun = findTasksToRun()
                val start = nowMillis()
                withTimeoutOrNull(pollingDelay.toMillis()) {
                    emitAll(tasksToRun)
                }
                val remainingDelay = pollingDelay - Duration.between(start, nowMillis())
                // If we have processed all tasks quickly, sleep until the next polling period.
                // Otherwise, read the next batch immediately
                if (remainingDelay > Duration.ZERO) {
                    delayFunction(pollingDelay)
                }
            }
        }
        if (concurrency > 0) {
            pendingTasksFlow.mapAsync(concurrency) { task ->
                runTask(task).join()
            }.launchIn(scope)
        } else {
            pendingTasksFlow.onEach(::runTask)
                .launchIn(scope)
        }
    }

    fun runTaskBatch() {
        if (concurrency > 0) {
            val currentlyRunning = runningTasksCount.get()
            if (currentlyRunning >= concurrency) {
                logger.info("Skip tasks run as runner is full. running=$currentlyRunning, concurrency=$concurrency")
                return
            }
        }
        scope.launch {
            logger.info("TaskHandler: find next batch of tasks to run")
            val tasksStarted = findTasksToRun()
                .let {
                    if (concurrency > 0) {
                        it.take(concurrency * 2)
                    } else {
                        it
                    }
                }
                .map(::runTask)
                .count()
            logger.info("TaskHandler: started a batch of $tasksStarted tasks")
        }
    }

    private fun findTasksToRun() = Flux.concat(
        taskRepository.findByRunningAndLastStatusOrderByPriorityDescIdAsc(false, TaskStatus.ERROR),
        taskRepository.findByRunningAndLastStatusOrderByPriorityDescIdAsc(false, TaskStatus.NONE)
    ).asFlow()

    private fun runTask(task: Task): Job {
        return runTask(task.type, task.param, task.sample).also {
            logger.info("TaskHandler: started task $task")
        }
    }

    private fun runTask(type: String, param: String, sample: Long? = Task.DEFAULT_SAMPLE): Job {
        return scope.launch {
            val handler = handlersMap[type]
            if (handler != null) {
                val count = runningTasksCount.incrementAndGet()
                if (concurrency > 0 && count > concurrency) {
                    logger.info(
                        "do not start task type=$type param=$param. " +
                            "Concurrency=$concurrency, running=${count - 1}"
                    )
                    runningTasksCount.decrementAndGet()
                    return@launch
                }
                logger.info("runTask type=$type param=$param")
                try {
                    runner.runLongTask(param, handler, sample)
                } finally {
                    runningTasksCount.decrementAndGet()
                }
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
