package com.rarible.core.task

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import reactor.core.publisher.Flux

@ExtendWith(MockKExtension::class)
@OptIn(FlowPreview::class)
@Suppress("ReactiveStreamsUnusedPublisher")
class TaskServiceStreamingTest {
    @InjectMockKs
    private lateinit var taskService: TaskService

    private val taskRepository: TaskRepository = mockk {
        coEvery { findByRunning(true) } returns Flux.empty()
    }

    @MockK
    private lateinit var runner: TaskRunner

    @MockK
    private lateinit var mongo: ReactiveMongoOperations

    @Suppress("unused")
    private val handlers = listOf(mockk<TaskHandler<String>> {
        every { type } returns "MOCK"
    })

    @Suppress("unused")
    private val raribleTaskProperties = RaribleTaskProperties(streaming = true, concurrency = 2)

    @Suppress("unused")
    private val meterRegistry = SimpleMeterRegistry()

    @Test
    fun `guarantees that a task with high priority will eventually be executed`() = runBlocking<Unit> {
        val dbTimeout = Channel<Unit>(Channel.RENDEZVOUS)

        coEvery {
            taskRepository.findByRunningAndLastStatusOrderByPriorityDescIdAsc(
                running = false,
                lastStatus = TaskStatus.ERROR
            )
        } returns Flux.empty()
        coEvery {
            taskRepository.findByRunningAndLastStatusOrderByPriorityDescIdAsc(
                running = false,
                lastStatus = TaskStatus.NONE
            )
        } returnsMany listOf(
            flow {
                emit(createTask("t2_1", 2))
                generateSequence(1) { it + 1 }.forEach { i ->
                    emit(createTask("t1_$i", 1))
                }
            }.asFlux(),
            flow {
                emit(createTask("t2_2", 2))
                generateSequence(1) { it + 1 }.forEach { i ->
                    emit(createTask("t0_$i", 0))
                }
            }.asFlux(),
        )

        val taskChannel2_1 = Channel<String>()
        coEvery {
            runner.runLongTask<String>("t2_1", any(), 0)
        } coAnswers {
            taskChannel2_1.send("ready")
            taskChannel2_1.receiveCatching()
            TaskRunStatus.SUCCESS
        }

        val taskChannel1 = Channel<String>()
        coEvery {
            runner.runLongTask<String>(match { it.startsWith("t1_") }, any(), 0)
        } coAnswers {
            taskChannel1.receiveCatching()
            TaskRunStatus.SUCCESS
        }

        val taskChannel2_2 = Channel<String>()
        coEvery {
            runner.runLongTask<String>("t2_2", any(), 0)
        } coAnswers {
            taskChannel2_2.send("ready")
            taskChannel2_2.receiveCatching()
            TaskRunStatus.SUCCESS
        }

        val taskChannel0 = Channel<String>()
        coEvery {
            runner.runLongTask<String>(match { it.startsWith("t0_") }, any(), 0)
        } coAnswers {
            taskChannel0.send("ready")
            taskChannel0.receiveCatching()
            TaskRunStatus.SUCCESS
        }

        val pollerJob = launch {
            taskService.runStreamingTaskPoller(
                sleep = { },
                dbTimeout = { dbTimeout.receive() },
            )
        }

        assertThat(taskChannel2_1.receive()).isEqualTo("ready")
        // Now task t2_1 and some t1_* tasks are running
        // Stop reading from DB
        dbTimeout.send(Unit)
        // Stop all t1_* tasks
        taskChannel1.close()

        // Now t2_2 should start
        assertThat(taskChannel2_2.receive()).isEqualTo("ready")
        // Now t2_1 finishes
        taskChannel2_1.close()

        // Expect at least one task with priority 0 to start
        assertThat(taskChannel0.receive()).isEqualTo("ready")

        pollerJob.cancel()
    }

    private fun createTask(param: String, priority: Int?) = Task(
        type = MOCK_TASK_TYPE,
        param = param,
        priority = priority,
        lastStatus = TaskStatus.NONE,
        state = null,
        running = false,
        sample = 0L,
    )
}
