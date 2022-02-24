package com.rarible.core.loader

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.configuration.LoadProperties
import com.rarible.core.loader.configuration.RetryProperties
import com.rarible.core.loader.internal.runner.LoadFatalError
import com.rarible.core.loader.internal.common.LoadMetrics
import com.rarible.core.loader.internal.notification.LoadNotificationKafkaSender
import com.rarible.core.loader.internal.runner.LoadRunner
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.loader.internal.common.LoadTaskService
import com.rarible.core.loader.internal.runner.LoadCriticalCodeExecutor
import com.rarible.core.loader.internal.common.toApiStatus
import com.rarible.core.loader.test.testLoaderType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.OptimisticLockingFailureException
import java.time.Clock

class LoadRunnerTest {

    private val loader = mockk<Loader> {
        every { type } returns testLoaderType
        coJustRun { load(any()) }
    }

    private val loadNotificationKafkaSender = mockk<LoadNotificationKafkaSender>()
    private val loadTaskService = mockk<LoadTaskService>()
    private val clock = mockk<Clock>()
    private val loadProperties = LoadProperties(
        brokerReplicaSet = "not used in the test",
        topicsPrefix = "loader-test",
        retry = RetryProperties(
            backoffDelaysMillis = listOf(1000, 2000) // 2 attempts to load after 1000 and 2000 ms.
        )
    )
    private val meterRegistry = SimpleMeterRegistry()
    private val loadMetrics = LoadMetrics(meterRegistry = meterRegistry)
    private val loadRunner = LoadRunner(
        loaders = listOf(loader),
        loadProperties = loadProperties,
        loadNotificationKafkaSender = loadNotificationKafkaSender,
        loadTaskService = loadTaskService,
        clock = clock,
        loadCriticalCodeExecutor = LoadCriticalCodeExecutor(
            retryAttempts = 5,
            backoffBaseDelay = 1
        ),
        loadMetrics = loadMetrics
    )

    @Test
    fun `scheduled task is loaded - update status to Loaded and send notification`() = runBlocking<Unit> {
        val scheduledTask = randomLoadTask(type = loader.type, status = randomScheduledStatus())
        coEvery { loadTaskService.get(scheduledTask.id) } returns scheduledTask
        val loadedAt = nowMillis()
        every { clock.instant() } returns loadedAt
        coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }
        coJustRun { loadNotificationKafkaSender.send(any()) }

        loadRunner.load(scheduledTask.id)

        val loadedStatus = LoadTask.Status.Loaded(
            scheduledAt = scheduledTask.status.scheduledAt,
            retryAttempts = 0,
            loadedAt = loadedAt
        )
        coVerify(exactly = 1) {
            loadTaskService.save(scheduledTask.copy(status = loadedStatus))
        }
        val loadNotification = LoadNotification(
            taskId = scheduledTask.id,
            type = scheduledTask.type,
            key = scheduledTask.key,
            status = loadedStatus.toApiStatus()
        )
        coVerify(exactly = 1) { loadNotificationKafkaSender.send(loadNotification) }
    }

    @Test
    fun `scheduled task is failed - update status to WaitsForRetry and send notification`() = runBlocking<Unit> {
        val scheduledTask = randomLoadTask(type = loader.type, status = randomScheduledStatus())
        coEvery { loadTaskService.get(scheduledTask.id) } returns scheduledTask
        val failedAt = nowMillis()
        val error = RuntimeException("test-error")
        coEvery { loader.load(any()) } throws error
        every { clock.instant() } returns failedAt
        coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }
        coJustRun { loadNotificationKafkaSender.send(any()) }

        loadRunner.load(scheduledTask.id)

        val waitsForRetryStatus = LoadTask.Status.WaitsForRetry(
            scheduledAt = scheduledTask.status.scheduledAt,
            retryAttempts = 0,
            failedAt = failedAt,
            retryAt = failedAt + loadProperties.retry.getRetryDelay(0),
            errorMessage = error.localizedMessage,
            rescheduled = false
        )
        coVerify(exactly = 1) {
            loadTaskService.save(scheduledTask.copy(status = waitsForRetryStatus))
        }
        val loadNotification = LoadNotification(
            taskId = scheduledTask.id,
            type = scheduledTask.type,
            key = scheduledTask.key,
            status = waitsForRetryStatus.toApiStatus()
        )
        coVerify(exactly = 1) { loadNotificationKafkaSender.send(loadNotification) }
    }

    @Test
    fun `retried task is loaded - update status to Loaded and send notification`() = runBlocking<Unit> {
        val waitsForRetryTask = randomLoadTask(
            type = loader.type,
            status = randomWaitsForRetryStatus().copy(retryAttempts = 1, rescheduled = false)
        )
        coEvery { loadTaskService.get(waitsForRetryTask.id) } returns waitsForRetryTask
        val loadedAt = nowMillis()
        coJustRun { loader.load(any()) }
        every { clock.instant() } returns loadedAt
        coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }
        coJustRun { loadNotificationKafkaSender.send(any()) }

        loadRunner.load(waitsForRetryTask.id)

        val loadedStatus = LoadTask.Status.Loaded(
            scheduledAt = waitsForRetryTask.status.scheduledAt,
            retryAttempts = 1,
            loadedAt = loadedAt
        )
        coVerify(exactly = 1) {
            loadTaskService.save(waitsForRetryTask.copy(status = loadedStatus))
        }
        val loadNotification = LoadNotification(
            taskId = waitsForRetryTask.id,
            type = waitsForRetryTask.type,
            key = waitsForRetryTask.key,
            status = loadedStatus.toApiStatus()
        )
        coVerify(exactly = 1) { loadNotificationKafkaSender.send(loadNotification) }
    }


    @Test
    fun `retried task is failed for the second time - update retryAt and retryAttempts and send notification`() =
        runBlocking<Unit> {
            val waitsForRetryTask = randomLoadTask(
                type = loader.type,
                status = randomWaitsForRetryStatus().copy(retryAttempts = 0, rescheduled = false)
            )
            coEvery { loadTaskService.get(waitsForRetryTask.id) } returns waitsForRetryTask
            val failedAt = nowMillis()
            val error = RuntimeException("test-error")
            coEvery { loader.load(any()) } throws error
            every { clock.instant() } returns failedAt
            coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }
            coJustRun { loadNotificationKafkaSender.send(any()) }

            loadRunner.load(waitsForRetryTask.id)

            val waitsForRetryStatus = LoadTask.Status.WaitsForRetry(
                scheduledAt = waitsForRetryTask.status.scheduledAt,
                retryAttempts = 1,
                failedAt = failedAt,
                retryAt = failedAt + loadProperties.retry.getRetryDelay(1),
                errorMessage = error.localizedMessage,
                rescheduled = false
            )
            coVerify(exactly = 1) {
                loadTaskService.save(waitsForRetryTask.copy(status = waitsForRetryStatus))
            }
            val loadNotification = LoadNotification(
                taskId = waitsForRetryTask.id,
                type = waitsForRetryTask.type,
                key = waitsForRetryTask.key,
                status = waitsForRetryStatus.toApiStatus()
            )
            coVerify(exactly = 1) { loadNotificationKafkaSender.send(loadNotification) }
        }

    @Test
    fun `retried task is failed for the last time - update status to Failed and send notification`() =
        runBlocking<Unit> {
            val waitsForRetryTask = randomLoadTask(
                type = loader.type,
                status = randomWaitsForRetryStatus().copy(retryAttempts = 2, rescheduled = false)
            )
            coEvery { loadTaskService.get(waitsForRetryTask.id) } returns waitsForRetryTask
            val failedAt = nowMillis()
            val error = RuntimeException("test-error")
            coEvery { loader.load(any()) } throws error
            every { clock.instant() } returns failedAt
            coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }
            coJustRun { loadNotificationKafkaSender.send(any()) }

            loadRunner.load(waitsForRetryTask.id)

            val failedStatus = LoadTask.Status.Failed(
                scheduledAt = waitsForRetryTask.status.scheduledAt,
                retryAttempts = 2,
                failedAt = failedAt,
                errorMessage = error.localizedMessage
            )
            coVerify(exactly = 1) {
                loadTaskService.save(waitsForRetryTask.copy(status = failedStatus))
            }
            val loadNotification = LoadNotification(
                taskId = waitsForRetryTask.id,
                type = waitsForRetryTask.type,
                key = waitsForRetryTask.key,
                status = failedStatus.toApiStatus()
            )
            coVerify(exactly = 1) { loadNotificationKafkaSender.send(loadNotification) }
        }

    @Test
    fun `do nothing on already loaded task`() = runBlocking<Unit> {
        val loadedTask = randomLoadTask(
            type = loader.type,
            status = randomLoadedStatus()
        )
        coEvery { loadTaskService.get(loadedTask.id) } returns loadedTask
        loadRunner.load(loadedTask.id)
        coVerify(exactly = 0) { loader.load(any()) }
    }

    @Test
    fun `do nothing on already failed task`() = runBlocking<Unit> {
        val loadedTask = randomLoadTask(
            type = loader.type,
            status = randomFailedStatus()
        )
        coEvery { loadTaskService.get(loadedTask.id) } returns loadedTask
        loadRunner.load(loadedTask.id)
        coVerify(exactly = 0) { loader.load(any()) }
    }

    @Test
    fun `fatal error - if exception on sending Loaded notification to Kafka`() = runBlocking<Unit> {
        val scheduledTask = randomLoadTask(type = loader.type, status = randomScheduledStatus())
        coEvery { loadTaskService.get(scheduledTask.id) } returns scheduledTask
        val loadedAt = nowMillis()
        every { clock.instant() } returns loadedAt
        coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }
        val kafkaError = RuntimeException("kafka-error")
        coEvery { loadNotificationKafkaSender.send(any()) } throws kafkaError

        assertThatThrownBy {
            runBlocking { loadRunner.load(scheduledTask.id) }
        }
            .isInstanceOf(LoadFatalError::class.java)
            .hasCause(kafkaError)
            .hasSuppressedException(kafkaError)
    }

    @Test
    fun `rethrow java lang Error from the Loader`(): Unit = runBlocking {
        val scheduledTask = randomLoadTask(type = loader.type, status = randomScheduledStatus())
        coEvery { loadTaskService.get(scheduledTask.id) } returns scheduledTask
        coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }

        val error = OutOfMemoryError()
        coEvery { loader.load(any()) } throws error

        assertThrows<OutOfMemoryError> { loadRunner.load(scheduledTask.id) }
    }

    @Test
    fun `ignore the task completion if another worker has finished the task concurrently`() = runBlocking<Unit> {
        val scheduledTask = randomLoadTask(type = loader.type, status = randomScheduledStatus())
        coEvery { loadTaskService.get(scheduledTask.id) } returns scheduledTask
        val loadedAt = nowMillis()
        every { clock.instant() } returns loadedAt
        coEvery { loadTaskService.save(any()) } throws OptimisticLockingFailureException("concurrent-update")
        coJustRun { loadNotificationKafkaSender.send(any()) }

        loadRunner.load(scheduledTask.id)

        val loadedStatus = LoadTask.Status.Loaded(
            scheduledAt = scheduledTask.status.scheduledAt,
            retryAttempts = 0,
            loadedAt = loadedAt
        )
        coVerify(exactly = 1) {
            loadTaskService.save(scheduledTask.copy(status = loadedStatus))
        }
        coVerify(exactly = 0) { loadNotificationKafkaSender.send(any()) }
    }

    /**
     * See Javadoc of [com.rarible.core.loader.internal.RetryTasksService].
     */
    @Test
    fun `retry to save the WaitsForRetry status task with rescheduled status`() = runBlocking<Unit> {
        val waitsForRetryStatus = randomWaitsForRetryStatus().copy(
            rescheduled = false,
            retryAttempts = 0
        )
        val waitsForRetryTask = randomLoadTask(
            type = loader.type,
            status = waitsForRetryStatus
        )
        val waitsForRetryTaskRescheduled = waitsForRetryTask.copy(
            status = waitsForRetryStatus.copy(rescheduled = true),
            version = 1
        )
        coEvery { loadTaskService.get(waitsForRetryTask.id) } returnsMany
                listOf(waitsForRetryTask, waitsForRetryTaskRescheduled)
        val loadedAt = nowMillis()
        every { clock.instant() } returns loadedAt
        coJustRun { loadNotificationKafkaSender.send(any()) }
        coEvery { loadTaskService.save(any()) } coAnswers {
            throw OptimisticLockingFailureException("RetryTasksService has updated the 'rescheduled' flag")
        } coAndThen {
            // On the second attempt, complete the task
            firstArg()
        }

        loadRunner.load(waitsForRetryTask.id)

        val loadedStatus = LoadTask.Status.Loaded(
            scheduledAt = waitsForRetryTask.status.scheduledAt,
            retryAttempts = 0,
            loadedAt = loadedAt
        )
        coVerify(exactly = 1) {
            loadTaskService.save(waitsForRetryTask.copy(status = loadedStatus))
        }

        coVerify(exactly = 1) {
            loadNotificationKafkaSender.send(
                LoadNotification(
                    taskId = waitsForRetryTask.id,
                    type = waitsForRetryTask.type,
                    key = waitsForRetryTask.key,
                    status = loadedStatus.toApiStatus()
                )
            )
        }
    }

}
