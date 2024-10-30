package com.rarible.core.task

import com.rarible.core.common.mapAsync
import com.rarible.core.common.nowMillis
import com.rarible.core.common.takeUntilTimeout
import com.rarible.core.telemetry.metrics.AbstractMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.util.NamedThreadFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.time.delay
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
@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val runner: TaskRunner,
    private val mongo: ReactiveMongoOperations,
    handlers: List<TaskHandler<*>>,
    raribleTaskProperties: RaribleTaskProperties,
    meterRegistry: MeterRegistry,
) {
    // todo use ForkJoinPool(concurrency) for better behavior on multiple small tasks
    private val taskDispatcher = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2,
        NamedThreadFactory("task-")
    ).asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + taskDispatcher)

    private val handlersMap = handlers.associateBy { it.type }
    private val runningTasksCount = AtomicInteger(0)
    private val streaming = raribleTaskProperties.streaming
    private val concurrency = raribleTaskProperties.concurrency
    private val pollingDelay = raribleTaskProperties.pollingPeriod

    private val metrics = TaskServiceMetrics(raribleTaskProperties.streaming, raribleTaskProperties.concurrency, meterRegistry)

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
                runTask(handler.type, param, sample, TaskServiceMetrics.TaskRunReason.AUTORUN)
            }
        }
    }

    suspend fun executeTasks() {
        if (streaming) {
            runStreamingTaskPoller()
        } else {
            runTaskBatch()
        }
    }

    internal suspend fun runStreamingTaskPoller(
        sleep: suspend (Duration) -> Unit = { delay(it) },
        dbTimeout: suspend (Duration) -> Unit = { delay(it) },
    ) {
        val pendingTasksFlow = flow {
            while (coroutineContext.isActive) {
                logger.info("TaskHandler: retrieving the next flow of tasks")
                metrics.onReadout()
                val tasksToRun = findTasksToRun().takeUntilTimeout(pollingDelay, dbTimeout)

                val start = nowMillis()
                var retrievedTasks = 0
                tasksToRun.collect { task ->
                    emit(task)
                    retrievedTasks += 1
                }
                val timeToProcess = Duration.between(start, nowMillis())
                logger.info("TaskHandler: retrieved {} tasks and processed some of them in {}", retrievedTasks, timeToProcess)
                val remainingDelay = pollingDelay - timeToProcess
                // If we have processed all tasks quickly, sleep until the next polling period.
                // Otherwise, re-read from DB immediately
                if (remainingDelay > Duration.ZERO) {
                    sleep(remainingDelay)
                }
            }
        }
        if (concurrency > 0) {
            pendingTasksFlow.mapAsync(concurrency) { task ->
                runTask(task).join()
                // Make sure that the dispatcher allows for parallelism so that we can join() in parallel
            }.flowOn(taskDispatcher)
                .collect {}
        } else {
            scope.launch {
                pendingTasksFlow.map(::runTask).collect {}
            }.join()
        }
    }

    internal suspend fun runTaskBatch() {
        if (concurrency > 0) {
            val currentlyRunning = runningTasksCount.get()
            if (currentlyRunning >= concurrency) {
                logger.info("Skip tasks run as runner is full. running=$currentlyRunning, concurrency=$concurrency")
                return
            }
        }
        scope.launch {
            logger.info("TaskHandler: find next batch of tasks to run")
            metrics.onReadout()
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
        }.join()
    }

    private fun findTasksToRun() = Flux.concat(
        taskRepository.findByRunningAndLastStatusOrderByPriorityDescIdAsc(false, TaskStatus.ERROR),
        taskRepository.findByRunningAndLastStatusOrderByPriorityDescIdAsc(false, TaskStatus.NONE)
    ).asFlow()

    private fun runTask(task: Task): Job {
        logger.info("TaskHandler: selected task $task")
        metrics.onTaskSelected()
        return runTask(task.type, task.param, task.sample, TaskServiceMetrics.TaskRunReason.PERIODIC)
    }

    fun runTask(type: String, param: String, sample: Long? = Task.DEFAULT_SAMPLE) {
        runTask(type, param, sample, TaskServiceMetrics.TaskRunReason.MANUAL)
    }

    private fun runTask(type: String, param: String, sample: Long?, runReason: TaskServiceMetrics.TaskRunReason): Job {
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
                // NB: can be inflated a bit because of the logic above
                metrics.onTaskCount(count)
                logger.info("runTask type=$type param=$param")
                metrics.onTaskStarted(runReason)
                var taskExecutionStatus = TaskRunStatus.NOT_EXECUTED
                try {
                    taskExecutionStatus = runner.runLongTask(param, handler, sample)
                } finally {
                    val countAfterRun = runningTasksCount.decrementAndGet()
                    metrics.onTaskFinished(runReason, taskExecutionStatus.toMetricsEnum())
                    metrics.onTaskCount(countAfterRun)
                }
            } else {
                logger.info("TaskHandler $type not found. skipping")
            }
        }
    }

    private fun TaskRunStatus.toMetricsEnum() = when (this) {
        TaskRunStatus.SUCCESS -> TaskServiceMetrics.ExecutionStatus.SUCCESS
        TaskRunStatus.FAILURE -> TaskServiceMetrics.ExecutionStatus.FAILURE
        TaskRunStatus.CANNOT_RUN -> TaskServiceMetrics.ExecutionStatus.CANNOT_RUN
        TaskRunStatus.NOT_EXECUTED -> TaskServiceMetrics.ExecutionStatus.NOT_EXECUTED
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

    // Visible for testing
    internal fun stopAllTasks() {
        scope.coroutineContext[Job]!!.children.forEach {
            logger.info("Stopping $it")
            it.cancel()
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TaskService::class.java)
    }
}

private class TaskServiceMetrics(
    streaming: Boolean,
    concurrency: Int,
    meterRegistry: MeterRegistry
) : AbstractMetrics(meterRegistry) {
    private val modeTag = tag(TAG_MODE, if (streaming) MODE_STREAMING else MODE_BATCH)
    private val concurrencyTag = tag(TAG_CONCURRENCY, concurrency.toString())
    private val tags = listOf(modeTag, concurrencyTag)

    private val readoutTotal = createCounter(READOUT_TOTAL, tags)
    private val tasksSelectedCounter = createCounter(SELECTED_TASKS_TOTAL, tags)
    private val runningTasksGauge = createGauge(RUNNING_TASKS, tags)

    fun onReadout() = readoutTotal.increment()

    fun onTaskSelected() = tasksSelectedCounter.increment()

    fun onTaskStarted(runReason: TaskRunReason) {
        increment(
            STARTED_TASKS_TOTAL,
            modeTag,
            concurrencyTag,
            tag(TAG_RUN_REASON, runReason.toString().lowercase()),
        )
    }

    fun onTaskFinished(runReason: TaskRunReason, executionStatus: ExecutionStatus) {
        increment(
            FINISHED_TASKS_TOTAL,
            modeTag,
            concurrencyTag,
            tag(TAG_RUN_REASON, runReason.toString().lowercase()),
            tag(TAG_EXECUTION_STATUS, executionStatus.toString().lowercase()),
        )
    }

    fun onTaskCount(count: Int) = runningTasksGauge.set(count.toDouble())

    private companion object {
        const val READOUT_TOTAL = "rarible_core_tasks_readout_total"
        const val SELECTED_TASKS_TOTAL = "rarible_core_tasks_selected_total"
        const val STARTED_TASKS_TOTAL = "rarible_core_tasks_started_total"
        const val FINISHED_TASKS_TOTAL = "rarible_core_tasks_finished_total"
        const val RUNNING_TASKS = "rarible_core_tasks_running"

        const val TAG_MODE = "mode"
        const val TAG_CONCURRENCY = "concurrency"
        const val TAG_RUN_REASON = "run_reason"
        const val TAG_EXECUTION_STATUS = "execution_status"

        const val MODE_STREAMING = "streaming"
        const val MODE_BATCH = "batch"
    }

    enum class ExecutionStatus {
        SUCCESS,
        FAILURE,
        CANNOT_RUN,
        NOT_EXECUTED,
    }

    enum class TaskRunReason {
        AUTORUN,
        PERIODIC,
        MANUAL,
    }
}
