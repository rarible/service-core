package com.rarible.core.task

import com.rarible.core.task.TestHandler.Companion.taskExecutionOrder
import com.rarible.core.test.wait.BlockingWait.waitAssert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@FlowPreview
@ExperimentalCoroutinesApi
@ContextConfiguration(classes = [PriorityContext::class])
@TestPropertySource(
    properties = [
        "rarible.core.task.concurrency=1",
        "rarible.core.task.initialDelay=PT1S",
        "rarible.core.task.pollingPeriod=PT1S"
    ]
)
@DirtiesContext
class RaribleTaskWorkerIt : AbstractIntegrationTest() {

    @Test
    fun `should prioritise tasks`() = runBlocking<Unit> {
        listOf(
            task("Type1", "null_1"),
            task("Type2", "p5", 5),
            task("Type2", "p8", 8),
            task("Type1", "null_2"),
            task("Type3", "p10", 10),
            task("Type4", "p7", 7),
        ).forEach { taskRepository.save(it).awaitFirst() }

        waitAssert(timeout = 15_000) {
            assertThat(taskExecutionOrder).containsExactly("p10", "p8", "p7", "p5", "null_1", "null_2")
        }
    }

    private fun task(type: String, param: String, priority: Int? = null) = Task(
        type = type,
        param = param,
        running = false,
        priority = priority
    )
}

@Configuration
@EnableRaribleTask
class PriorityContext {
    @Bean
    fun handler1() = TestHandler("Type1")

    @Bean
    fun handler2() = TestHandler("Type2")

    @Bean
    fun handler3() = TestHandler("Type3")

    @Bean
    fun handler4() = TestHandler("Type4")
}

class TestHandler(override val type: String) : TaskHandler<Int> {
    override fun runLongTask(from: Int?, param: String): Flow<Int> {
        taskExecutionOrder.add(param)
        return emptyFlow()
    }

    companion object {
        val taskExecutionOrder = mutableListOf<String>()
    }
}
