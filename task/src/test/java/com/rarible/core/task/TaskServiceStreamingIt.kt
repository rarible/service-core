package com.rarible.core.task

import com.rarible.core.common.nowMillis
import com.rarible.core.test.hasProperty
import com.rarible.core.test.wait.Wait.waitAssert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.time.Duration

@FlowPreview
@ExperimentalCoroutinesApi
@ContextConfiguration(classes = [MockContext::class])
@TestPropertySource(
    properties = [
        "rarible.core.task.concurrency=2",
        "rarible.core.task.streaming=true",
        // Do not want RaribleTaskWorker - we want to run tasks on our own
        "rarible.core.task.enabled=false",
    ]
)
@DirtiesContext
class TaskServiceStreamingIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var handler: MockHandler

    @Autowired
    private lateinit var service: TaskService

    @Value("\${rarible.core.task.concurrency}")
    private lateinit var concurrencyString: String

    private val concurrency: Int by lazy { concurrencyString.toInt() }

    private val sleepChannel = Channel<ExpectedDelayBoundaries>(Channel.RENDEZVOUS)
    private val pollingDelay = Duration.ofMinutes(1)

    @AfterEach
    fun tearDown() {
        service.stopAllTasks()
    }

    @Test
    fun executesInParallel() = runBlocking<Unit> {
        scheduleTask("p1")
        scheduleTask("p2")
        scheduleTask("p3")

        val pollerJob = launch {
            service.runStreamingTaskPoller(
                pollingDelay = pollingDelay,
                sleep = ::verifyingSleepFunction
            )
        }

        waitAssert {
            checkIsRunning("p1", true, TaskStatus.NONE)
            checkIsRunning("p2", true, TaskStatus.NONE)
        }

        val v1 = RandomUtils.nextInt()
        handler.sendMessage("p1", v1)
        val v2 = RandomUtils.nextInt()
        handler.sendMessage("p2", v2)

        waitAssert {
            checkState("p1", v1)
            checkState("p2", v2)

            // Two tasks are already running, cannot take this
            checkIsRunning("p3", false, TaskStatus.NONE)
        }

        handler.close("p1")
        handler.close("p2", IllegalStateException())

        waitAssert {
            checkIsRunning("p1", false, TaskStatus.COMPLETED)
            checkIsRunning("p2", false, TaskStatus.ERROR)
            checkIsRunning("p3", true, TaskStatus.NONE)
        }

        val v3 = RandomUtils.nextInt()
        handler.sendMessage("p3", v3)

        waitAssert {
            checkState("p3", v3)
        }

        val list = service.findTasks(MOCK_TASK_TYPE).toList()
        assertThat(list).hasSize(3)

        pollerJob.cancel()
    }

    private suspend fun verifyingSleepFunction(delayDuration: Duration) {
        val expectedDelayBoundaries = sleepChannel.receive()
        assertThat(delayDuration)
            .isGreaterThanOrEqualTo(expectedDelayBoundaries.min)
            .isLessThan(expectedDelayBoundaries.maxExcl)
    }

    private class ExpectedDelayBoundaries(
        val min: Duration,
        val maxExcl: Duration,
    )

    private suspend fun checkIsRunning(param: String, isRunning: Boolean, lastStatus: TaskStatus) {
        val t = taskRepository.findByTypeAndParam(MOCK_TASK_TYPE, param).awaitSingle()
        assertThat(t)
            .hasProperty(Task::lastStatus, lastStatus)
            .hasProperty(Task::running, isRunning)
    }

    private suspend fun checkState(param: String, state: Int) {
        val t = taskRepository.findByTypeAndParam(MOCK_TASK_TYPE, param).awaitSingle()
        assertThat(t).hasProperty(Task::state, state)
    }

    @Test
    fun `does not take more than concurrency from the DB`() = runBlocking<Unit> {
        repeat(2 * concurrency) { id ->
            scheduleTask("t$id")
        }

        val pollerJob = launch {
            service.runStreamingTaskPoller(
                pollingDelay = pollingDelay,
                sleep = ::verifyingSleepFunction
            )
        }

        val runningTasks1 = assertRunningTasksSize(concurrency)

        val id1 = runningTasks1.first().param
        handler.sendMessage(id1, 1)
        handler.close(id1)

        // 1 new task should be running now
        val idNew1 = assertRunningTasksSize(concurrency)
            .apply { removeAll(runningTasks1) }
            .single().param

        waitAssert {
            checkIsRunning(id1, isRunning = false, lastStatus = TaskStatus.COMPLETED)
            checkIsRunning(idNew1, isRunning = true, lastStatus = TaskStatus.NONE)
        }

        handler.sendMessage(idNew1, 10)
        waitAssert {
            checkState(idNew1, 10)
        }

        runningTasks1.drop(1).forEach { task ->
            handler.close(task.param)
        }

        // All the rest of the tasks are executed now
        val lastId = assertRunningTasksSize(concurrency)
            .let { tasks ->
                // Finish all of them but 1
                tasks.drop(1).forEach { task ->
                    handler.close(task.param)
                }
                tasks.first().param
            }

        // lastId is still running
        assertRunningTasksSize(1)

        handler.close(lastId)
        assertRunningTasksSize(0)

        pollerJob.cancel()
    }

    @Test
    fun `takes tasks as they appear`() = runBlocking<Unit> {
        scheduleTask("t1")

        val start = nowMillis()

        val pollerJob = launch {
            service.runStreamingTaskPoller(
                pollingDelay = pollingDelay,
                sleep = ::verifyingSleepFunction
            )
        }

        waitAssert {
            checkIsRunning("t1", isRunning = true, lastStatus = TaskStatus.NONE)
        }

        handler.close("t1")

        waitAssert {
            checkIsRunning("t1", isRunning = false, lastStatus = TaskStatus.COMPLETED)
        }

        scheduleTask("t2")

        // The first DB read has already finished, and pollingPeriod is not finished yet, so the task isn't started yet
        checkIsRunning("t2", isRunning = false, lastStatus = TaskStatus.NONE)

        sleepChannel.send(
            ExpectedDelayBoundaries(
                min = pollingDelay - Duration.between(start, nowMillis()),
                maxExcl = pollingDelay,
            )
        )

        waitAssert {
            checkIsRunning("t2", isRunning = true, lastStatus = TaskStatus.NONE)
        }

        handler.sendMessage("t2", 2)
        handler.close("t2")

        waitAssert {
            checkIsRunning("t2", isRunning = false, lastStatus = TaskStatus.COMPLETED)
            checkState("t2", 2)
        }

        assertRunningTasksSize(0)

        pollerJob.cancel()
    }

    private suspend fun scheduleTask(param: String, priority: Int? = null) {
        taskRepository.save(
            Task(
                type = MOCK_TASK_TYPE,
                param = param,
                priority = priority,
                lastStatus = TaskStatus.NONE,
                state = null,
                running = false,
                sample = 0L,
            )
        ).awaitSingle()
    }

    private suspend fun assertRunningTasksSize(size: Int): MutableList<Task> {
        waitAssert {
            assertThat(
                taskRepository.findByRunning(true).count().awaitSingle().toInt()
            ).isEqualTo(size)
        }
        // Give time to allow more tasks to spin up, if there are such
        delay(300)
        val runningTasks = taskRepository.findByRunning(true).collectList().awaitSingle()
        assertThat(runningTasks).hasSize(size)
        return runningTasks
    }
}
