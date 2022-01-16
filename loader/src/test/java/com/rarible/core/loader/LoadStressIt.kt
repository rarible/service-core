package com.rarible.core.loader

import com.rarible.core.loader.test.testLoaderType
import com.rarible.core.loader.test.testReceivedNotifications
import com.rarible.core.test.wait.Wait
import io.mockk.coEvery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Disabled
class LoadStressIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var loadService: LoadService

    @ExperimentalTime
    @Test
    fun `stress test loading`() = runBlocking<Unit> {
        println(clock.instant())
        coEvery { loader.load(any()) } coAnswers {
            val key = firstArg<String>()
            val id = key.substringBefore(":").toLong()
            val delay = key.substringAfter(":").toLong()
            delay(delay)
            println("Finished task $id after $delay")
        }
        val numberOfTasks = 1000
        val loadTasks = (0 until numberOfTasks).map { key ->
            val delay = key * 2
            testLoaderType to "$delay:$delay"
        }
        val totalDelay = (0 until numberOfTasks).sumOf { it * 2 }
        println("Total tasks delay: $totalDelay")
        println("Number of workers: ${loadProperties.workers}")
        println("Number of task topic partitions: ${loadProperties.loadTasksTopicPartitions}")
        loadTasks.forEach { loadService.scheduleLoad(it.first, it.second) }
        val totalTime = measureTime {
            Wait.waitAssert(timeout = Duration.ofMinutes(1)) {
                assertThat(testReceivedNotifications.filter { it.status is LoadTaskStatus.Scheduled }).hasSize(numberOfTasks)
                assertThat(testReceivedNotifications.filter { it.status is LoadTaskStatus.Loaded }).hasSize(numberOfTasks)
            }
        }
        println("Total execution time: $totalTime")
        val speedupFactor = totalDelay.toDouble() / totalTime.inWholeMilliseconds.toDouble()
        println("Speedup factor for ${loadProperties.workers} workers " + String.format("%.2f", speedupFactor))
    }
}
