package com.rarible.core.task

import com.rarible.core.test.wait.Wait
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration

@FlowPreview
@ExperimentalCoroutinesApi
class RaribleTaskWorkerTest {
    private val taskService = mockk<TaskService>()
    private val properties = RaribleTaskProperties(
        true, Duration.ZERO, Duration.ZERO, Duration.ZERO
    )
    private val meterRegistry = SimpleMeterRegistry()

    private val worker = RaribleTaskWorker(
        taskService = taskService,
        properties = properties,
        meterRegistry = meterRegistry,
    )

    @Test
    fun `handle init delay`() = runBlocking<Unit> {
        every { taskService.autorun() } returns Unit
        every { taskService.runTasks() } returns Unit

        worker.onApplicationStarted()

        Wait.waitAssert {
            verify(exactly = 1) { taskService.autorun() }
            verify(atLeast = 5) { taskService.runTasks() }
        }
        worker.close()
    }
}