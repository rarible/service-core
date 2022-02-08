package com.rarible.core.loader

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.internal.LoadMetrics
import com.rarible.core.loader.internal.LoadTaskId
import com.rarible.core.loader.internal.RetryTasksService
import com.rarible.core.loader.test.testLoaderType
import com.rarible.core.loader.test.testReceivedNotifications
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

class LoadIt : AbstractIntegrationTest() {
    @Autowired
    lateinit var loadService: LoadService

    @Autowired
    lateinit var retryTasksService: RetryTasksService

    @Autowired
    lateinit var loadMetrics: LoadMetrics

    @BeforeEach
    fun clearMetrics() {
        loadMetrics.reset()
    }

    @Test
    fun `load - then receive Loaded notification - then get Loaded status`() = runBlocking<Unit> {
        val loadKey = randomString()
        coEvery { loader.load(loadKey) } coAnswers {
            delay(500)
        }
        val scheduledAt = nowMillis()
        val loadedAt = scheduledAt.plusSeconds(1)
        every { clock.instant() } returnsMany (listOf(scheduledAt, loadedAt))
        val loadTaskId = loadService.scheduleLoad(testLoaderType, loadKey)
        assertThat(loadService.getStatus(loadTaskId)).isEqualTo(
            LoadTaskStatus.Scheduled(
                scheduledAt = scheduledAt
            )
        )
        Wait.waitAssert {
            coVerify(exactly = 1) { loader.load(loadKey) }
            val status = LoadTaskStatus.Loaded(
                scheduledAt = scheduledAt,
                loadedAt = loadedAt,
                retryAttempts = 0
            )
            assertThat(testReceivedNotifications).containsExactly(
                LoadNotification(
                    taskId = loadTaskId,
                    type = testLoaderType,
                    key = loadKey,
                    status = status
                )
            )
            assertThat(loadService.getStatus(loadTaskId)).isEqualTo(status)
        }
        assertThat(loadMetrics.getNumberOfScheduledTasks(testLoaderType, false)).isEqualTo(1)
        assertThat(loadMetrics.getNumberOfFinishedTasks(testLoaderType, true)).isEqualTo(1)
    }

    @Test
    fun `one load exception - then retry - get Failed and then Loaded notification and status`() = runBlocking<Unit> {
        val loadKey = randomString()
        val exception = RuntimeException("test")
        coEvery { loader.load(loadKey) } throws (exception)
        val scheduledAt = nowMillis()
        val failedAt = scheduledAt.plusSeconds(1)
        every { clock.instant() } returnsMany (listOf(scheduledAt, failedAt))
        val loadTaskId = runBlocking { loadService.scheduleLoad(testLoaderType, loadKey) }
        Wait.waitAssert {
            coVerify(exactly = 1) { loader.load(loadKey) }
            val retryAt = failedAt + loadProperties.retry.getRetryDelay(0)
            val status = LoadTaskStatus.WaitsForRetry(
                scheduledAt = scheduledAt,
                retryAttempts = 0,
                failedAt = failedAt,
                errorMessage = exception.localizedMessage,
                retryAt = retryAt
            )
            assertThat(loadService.getStatus(loadTaskId)).isEqualTo(status)
            assertThat(testReceivedNotifications).isEqualTo(
                listOf(
                    LoadNotification(
                        taskId = loadTaskId,
                        type = testLoaderType,
                        key = loadKey,
                        status = status
                    )
                )
            )
        }

        testReceivedNotifications.clear()
        clearMocks(loader, clock)
        coJustRun { loader.load(loadKey) }

        // Trigger tasks retrying.
        val retryTime = scheduledAt.plusSeconds(2)
        val loadedAt = retryTime.plusSeconds(1)
        every { clock.instant() } returnsMany (listOf(retryTime, loadedAt))
        retryTasksService.scheduleTasksToRetry()

        Wait.waitAssert {
            coVerify(exactly = 1) { loader.load(loadKey) }
            val status = LoadTaskStatus.Loaded(
                scheduledAt = scheduledAt,
                retryAttempts = 0,
                loadedAt = loadedAt
            )
            assertThat(loadService.getStatus(loadTaskId)).isEqualTo(status)
            assertThat(testReceivedNotifications).isEqualTo(
                listOf(
                    LoadNotification(
                        taskId = loadTaskId,
                        type = testLoaderType,
                        key = loadKey,
                        status = status
                    )
                )
            )
        }
        assertThat(loadMetrics.getNumberOfScheduledTasks(testLoaderType, false)).isEqualTo(1)
        assertThat(loadMetrics.getNumberOfScheduledTasks(testLoaderType, true)).isEqualTo(1)
        assertThat(loadMetrics.getNumberOfFinishedTasks(testLoaderType, false)).isEqualTo(1)
        assertThat(loadMetrics.getNumberOfFinishedTasks(testLoaderType, true)).isEqualTo(1)
    }

    @Test
    fun `retry N times but finally fail - receive Failed notifications and statuses`() = runBlocking<Unit> {
        val loadKey = randomString()
        val scheduledAt = nowMillis()

        val exceptionChannel = Channel<RuntimeException>()
        coEvery { loader.load(loadKey) } coAnswers {
            val nextException = exceptionChannel.receive()
            throw nextException
        }

        // Blocking queue of "current" times.
        val currentTime = AtomicReference<Instant>()
        every { clock.instant() } answers { currentTime.get() }

        suspend fun verifyFailedStatusAndNotification(
            loadTaskId: LoadTaskId,
            retryAttempts: Int,
            failedAt: Instant,
            errorMessage: String
        ) {
            Wait.waitAssert {
                coVerify(exactly = 1 + retryAttempts) { loader.load(loadKey) }
                val expectedStatus = if (retryAttempts < loadProperties.retry.retryAttempts) {
                    LoadTaskStatus.WaitsForRetry(
                        scheduledAt = scheduledAt,
                        retryAttempts = retryAttempts,
                        failedAt = failedAt,
                        errorMessage = errorMessage,
                        retryAt = failedAt + loadProperties.retry.getRetryDelay(retryAttempts)
                    )
                } else {
                    LoadTaskStatus.Failed(
                        scheduledAt = scheduledAt,
                        retryAttempts = retryAttempts,
                        failedAt = failedAt,
                        errorMessage = errorMessage
                    )
                }

                assertThat(loadService.getStatus(loadTaskId)).isEqualTo(expectedStatus)
                assertThat(testReceivedNotifications).isEqualTo(
                    listOf(
                        LoadNotification(
                            taskId = loadTaskId,
                            type = testLoaderType,
                            key = loadKey,
                            status = expectedStatus
                        )
                    )
                )
            }
        }

        currentTime.set(scheduledAt)
        val loadTaskId = loadService.scheduleLoad(testLoaderType, loadKey)

        for (retryAttempts in 0 until loadProperties.retry.retryAttempts) {
            testReceivedNotifications.clear()

            // Update currentTime (and failedAt).
            currentTime.set(currentTime.get().plusSeconds(1))

            // Trigger the exception in the loader.
            val exception = RuntimeException("test-$retryAttempts")
            exceptionChannel.send(exception)

            val failedAt = currentTime.get()
            verifyFailedStatusAndNotification(
                loadTaskId = loadTaskId,
                retryAttempts = retryAttempts,
                failedAt = failedAt,
                errorMessage = exception.localizedMessage
            )

            // Schedule retry.
            currentTime.set(currentTime.get().plusSeconds(60))
            retryTasksService.scheduleTasksToRetry()
        }
    }
}
