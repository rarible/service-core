package com.rarible.core.task

import com.rarible.core.test.wait.BlockingWait.waitAssert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@FlowPreview
@ExperimentalCoroutinesApi
class TaskServiceTest : AbstractIntegrationTest() {
    @Autowired
    @Qualifier("mockHandler1")
    private lateinit var handler1: MockHandler
    @Autowired
    @Qualifier("mockHandler2")
    private lateinit var handler2: MockHandler
    @Autowired
    private lateinit var service: TaskService

    @Test
    fun executesInParallel() {
        runBlocking {
            taskRepository.save(Task(
                type = "MOCK1",
                param = "p1",
                lastStatus = TaskStatus.NONE,
                state = null,
                running = false
            )).awaitFirst()
            taskRepository.save(Task(
                type = "MOCK2",
                param = "p2",
                lastStatus = TaskStatus.NONE,
                state = null,
                running = false
            )).awaitFirst()
        }

        runBlocking {
            service.runTasks()
        }

        waitAssert {
            val t1 = taskRepository.findByTypeAndParam("MOCK1", "p1").block()!!
            assertThat(t1)
                .hasFieldOrPropertyWithValue(Task::lastStatus.name, TaskStatus.NONE)
            assertThat(t1)
                .hasFieldOrPropertyWithValue(Task::running.name, true)

            val t2 = taskRepository.findByTypeAndParam("MOCK2", "p2").block()!!
            assertThat(t2)
                .hasFieldOrPropertyWithValue(Task::lastStatus.name, TaskStatus.NONE)
            assertThat(t2)
                .hasFieldOrPropertyWithValue(Task::running.name, true)
        }

        val v1 = RandomUtils.nextInt()
        handler1.sink.next(v1)
        val v2 = RandomUtils.nextInt()
        handler2.sink.next(v2)

        waitAssert {
            val t1 = taskRepository.findByTypeAndParam("MOCK1", "p1").block()!!
            assertThat(t1)
                .hasFieldOrPropertyWithValue(Task::state.name, v1)

            val t2 = taskRepository.findByTypeAndParam("MOCK2", "p2").block()!!
            assertThat(t2)
                .hasFieldOrPropertyWithValue(Task::state.name, v2)
        }

        handler1.sink.complete()
        handler2.sink.error(IllegalStateException())

        waitAssert {
            val t1 = runBlocking { service.findTasks("MOCK1", "p1").first() }
            assertThat(t1)
                .hasFieldOrPropertyWithValue(Task::lastStatus.name, TaskStatus.COMPLETED)

            val t2 = runBlocking { service.findTasks("MOCK2", "p2").first() }
            assertThat(t2)
                .hasFieldOrPropertyWithValue(Task::lastStatus.name, TaskStatus.ERROR)
        }

        val list = runBlocking { service.findTasks("MOCK1").toList() }
        assertThat(list).hasSize(1)
    }
}
