package com.rarible.core.task

import com.rarible.core.test.eq
import com.rarible.core.test.extracting
import com.rarible.core.test.hasProperty
import com.rarible.core.test.wait.Wait.waitAssert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@FlowPreview
@ExperimentalCoroutinesApi
@ContextConfiguration(classes = [MockContext::class])
@TestPropertySource(properties = ["rarible.core.task.concurrency=2"])
@DirtiesContext
class TaskServiceTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var handler: MockHandler

    @Autowired
    private lateinit var service: TaskService

    @Value("\${rarible.core.task.concurrency}")
    private lateinit var concurrencyString: String

    private val concurrency: Int by lazy { concurrencyString.toInt() }

    @Test
    fun executesInParallel() = runBlocking<Unit> {
        scheduleTask("p1")
        scheduleTask("p2")

        service.runTasks()

        waitAssert {
            val t1 = taskRepository.findByTypeAndParam(MOCK_TASK_TYPE, "p1").awaitSingle()
            assertThat(t1)
                .hasFieldOrPropertyWithValue(Task::lastStatus.name, TaskStatus.NONE)
            assertThat(t1)
                .hasFieldOrPropertyWithValue(Task::running.name, true)

            val t2 = taskRepository.findByTypeAndParam(MOCK_TASK_TYPE, "p2").awaitSingle()
            assertThat(t2)
                .hasFieldOrPropertyWithValue(Task::lastStatus.name, TaskStatus.NONE)
            assertThat(t2)
                .hasFieldOrPropertyWithValue(Task::running.name, true)
        }

        val v1 = RandomUtils.nextInt()
        handler.sendMessage("p1", v1)
        val v2 = RandomUtils.nextInt()
        handler.sendMessage("p2", v2)

        waitAssert {
            val t1 = taskRepository.findByTypeAndParam(MOCK_TASK_TYPE, "p1").awaitSingle()
            assertThat(t1)
                .hasFieldOrPropertyWithValue(Task::state.name, v1)

            val t2 = taskRepository.findByTypeAndParam(MOCK_TASK_TYPE, "p2").awaitSingle()
            assertThat(t2)
                .hasFieldOrPropertyWithValue(Task::state.name, v2)
        }

        handler.close("p1")
        handler.close("p2", IllegalStateException())

        waitAssert {
            val t1 = service.findTasks(MOCK_TASK_TYPE, "p1").first()
            assertThat(t1)
                .hasFieldOrPropertyWithValue(Task::lastStatus.name, TaskStatus.COMPLETED)

            val t2 = service.findTasks(MOCK_TASK_TYPE, "p2").first()
            assertThat(t2)
                .hasFieldOrPropertyWithValue(Task::lastStatus.name, TaskStatus.ERROR)
        }

        val list = service.findTasks(MOCK_TASK_TYPE).toList()
        assertThat(list).hasSize(2)
    }

    @Test
    fun `does not take more than concurrency from the DB`() = runBlocking<Unit> {
        repeat(3 * concurrency - 1) { id ->
            scheduleTask("t$id")
        }

        service.runTasks()

        val runningTasks = assertRunningTasksSize(concurrency)

        val id1 = runningTasks.first().param
        handler.sendMessage(id1, 1)
        handler.close(id1)

        assertRunningTasksSize(concurrency - 1)

        assertThat(
            taskRepository.findByTypeAndParam(MOCK_TASK_TYPE, id1).awaitSingle()
        ).hasProperty(Task::lastStatus) { eq(TaskStatus.COMPLETED) }

        runningTasks.drop(1).forEach { task ->
            handler.close(task.param)
        }

        assertRunningTasksSize(0)

        service.runTasks()

        // Next batch of tasks should start.
        // Note that task service could have read from the DB more than concurrency, they should start now
        assertRunningTasksSize(concurrency)
            // Finish all of them
            .forEach { task ->
                handler.close(task.param)
            }

        assertRunningTasksSize(0)

        // Next batch of tasks should be executed
        service.runTasks()
        assertRunningTasksSize(concurrency - 1)
            .forEach { task ->
                handler.close(task.param)
            }
    }

    @Test
    fun `guarantees order by priority`() = runBlocking<Unit> {
        scheduleTask("tNULL_1", priority = null)
        repeat(concurrency) { id -> scheduleTask("t0_$id", priority = 0) }
        repeat(concurrency * 2 - 2) { id -> scheduleTask("t1_$id", priority = 1) }
        scheduleTask("t2_1", priority = 2)

        suspend fun assertBatch(expectedPriorities: List<Int?>) {
            val runningTasksBatch = assertRunningTasksSize(expectedPriorities.size)
            assertThat(runningTasksBatch).extracting(Task::priority) {
                containsExactlyInAnyOrderElementsOf(expectedPriorities)
            }
            runningTasksBatch.forEach { handler.close(it.param) }
            assertRunningTasksSize(0)
        }

        service.runTasks()
        assertBatch(listOf(2) + List(concurrency - 1) { 1 })

        scheduleTask("t2_2", priority = 2)
        service.runTasks()
        assertBatch(listOf(2) + List(concurrency - 1) { 1 })

        service.runTasks()
        assertBatch(List(concurrency) { 0 })

        service.runTasks()
        assertBatch(listOf(null))
    }

    private suspend fun scheduleTask(param: String, priority: Int? = null) {
        taskRepository.save(
            Task(
                type = MOCK_TASK_TYPE,
                param = param,
                priority = priority,
                lastStatus = TaskStatus.NONE,
                state = null,
                running = false
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
