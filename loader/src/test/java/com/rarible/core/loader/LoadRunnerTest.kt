package com.rarible.core.loader

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.configuration.LoadProperties
import com.rarible.core.loader.configuration.RetryProperties
import com.rarible.core.loader.internal.LoadNotificationKafkaSender
import com.rarible.core.loader.internal.LoadRunner
import com.rarible.core.loader.internal.LoadFatalError
import com.rarible.core.loader.internal.KafkaLoadTaskId
import com.rarible.core.loader.internal.LoadTask
import com.rarible.core.loader.internal.LoadTaskService
import com.rarible.core.loader.internal.LoaderCriticalCodeExecutor
import com.rarible.core.loader.internal.generateLoadTaskId
import com.rarible.core.loader.test.testLoaderType
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.time.Clock

/*
class LoadRunnerTest {

    private val loader = mockk<Loader> {
        every { type } returns testLoaderType
    }

    private val loadNotificationKafkaSender = mockk<LoadNotificationKafkaSender>()
    private val loadTaskService = mockk<LoadTaskService>()
    private val clock = mockk<Clock>()
    private val loadProperties = LoadProperties(
        brokerReplicaSet = "not used in test",
        retry = RetryProperties(
            backoffDelaysMillis = listOf(1000, 2000) // 2 attempts to load after 1000 and 2000 ms.
        )
    )
    private val loadRunner = LoadRunner(
        loaders = listOf(loader),
        loadProperties = loadProperties,
        loadNotificationKafkaSender = loadNotificationKafkaSender,
        loadTaskService = loadTaskService,
        clock = clock,
        loaderCriticalCodeExecutor = LoaderCriticalCodeExecutor(
            retryAttempts = 5,
            backoffBaseDelay = 1
        )
    )

    @Test
    fun `success - send notification`(): Unit = runBlocking {
        coJustRun { loader.load(any()) }
        coJustRun { loadNotificationKafkaSender.send(any()) }
        val now = nowMillis()
        every { clock.instant() } returns now
        val loadKey = "key"
        val loadTaskId = generateLoadTaskId()
        coEvery { loadTaskService.get(loadTaskId) } returns LoadTask(
            id = loadTaskId,
            type = testLoaderType,
            key = loadKey,
            attempts = 0, // Already the second (of two) retry attempt.
            scheduledAt = now,
            failedAt = null,
            retryAt = null
        )
        val loadTask = KafkaLoadTaskId(
            id = loadTaskId
        )
        loadRunner.load(loadTask)
        coVerify(exactly = 1) {
            loadNotificationKafkaSender.send(
                LoadNotification.Completed(
                    type = testLoaderType,
                    key = loadKey,
                    attempts = 0,
                    scheduledAt = now
                )
            )
        }
    }

    @Test
    fun `simple exception from the loader - schedule for retry later`(): Unit = runBlocking {
        val exception = RuntimeException("error-message")
        coEvery { loader.load(any()) } throws exception
        val now = nowMillis()
        every { clock.instant() } returns now
        coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }
        coJustRun { loadNotificationKafkaSender.send(any()) }
        val loadKey = "key"
        val loadTaskId = generateLoadTaskId()
        coEvery { loadTaskService.get(loadTaskId) } returns LoadTask(
            id = loadTaskId,
            type = testLoaderType,
            key = loadKey,
            attempts = 0, // Already the second (of two) retry attempt.
            scheduledAt = now,
            failedAt = null,
            retryAt = null
        )
        val loadTask = KafkaLoadTaskId(
            id = loadTaskId,
        )
        loadRunner.load(loadTask)
        coVerify(exactly = 1) {
            loadNotificationKafkaSender.send(
                LoadNotification.Failed(
                    type = testLoaderType,
                    key = loadKey,
                    errorMessage = exception.localizedMessage,
                    attempts = 0,
                    willBeRetried = true,
                    scheduledAt = now
                )
            )
        }
        coVerify(exactly = 1) {
            loadTaskService.save(match {
                it.type == loadTask.type
                        && it.key == loadTask.key
                        && it.retryAt == now.plusMillis(1000)
            })
        }
    }

    @Test
    fun `simple exception from the loader - mark task failed if ran out of attempts`(): Unit = runBlocking {
        val exception = RuntimeException("error-message")
        coEvery { loader.load(any()) } throws exception
        val now = nowMillis()
        every { clock.instant() } returns now
        coEvery { loadTaskService.save(any()) } coAnswers { firstArg() }
        coJustRun { loadNotificationKafkaSender.send(any()) }
        val loadTaskId = generateLoadTaskId()
        val loadKey = "key"
        coEvery { loadTaskService.get(loadTaskId) } returns LoadTask(
            id = loadTaskId,
            type = testLoaderType,
            key = loadKey,
            attempts = 2, // Already the second (of two) retry attempt.
            scheduledAt = now,
            failedAt = null,
            retryAt = null
        )
        val loadTask = KafkaLoadTaskId(
            id = loadTaskId
        )
        loadRunner.load(loadTask)
        coVerify(exactly = 1) {
            loadNotificationKafkaSender.send(
                LoadNotification.Failed(
                    type = testLoaderType,
                    key = loadKey,
                    attempts = 2,
                    errorMessage = exception.localizedMessage,
                    willBeRetried = false,
                    scheduledAt = now
                )
            )
        }
        coVerify(exactly = 1) {
            loadTaskService.save(match {
                it.type == loadTask.type && it.key == loadTask.key
            })
        }
    }

    @Test
    fun `fatal error - if exception on sending notification to kafka`() {
        coJustRun { loader.load(any()) }
        val kafkaException = IOException()
        coEvery { loadNotificationKafkaSender.send(any()) } throws kafkaException
        val now = nowMillis()
        every { clock.instant() } returns now
        val loadKey = "key"
        val loadTaskId = generateLoadTaskId()
        coEvery { loadTaskService.get(loadTaskId) } returns LoadTask(
            id = loadTaskId,
            type = testLoaderType,
            key = loadKey,
            attempts = 2, // On the second attempt.
            scheduledAt = now,
            failedAt = null,
            retryAt = null
        )
        val loadTask = KafkaLoadTaskId(
            id = loadTaskId
        )
        Assertions.assertThatThrownBy {
            runBlocking { loadRunner.load(loadTask) }
        }
            .isInstanceOf(LoadFatalError::class.java)
            .hasCause(kafkaException)
            .hasSuppressedException(kafkaException)

        coVerify(exactly = 5) {
            loadNotificationKafkaSender.send(
                LoadNotification.Completed(
                    type = testLoaderType,
                    key = loadKey,
                    attempts = 2,
                    scheduledAt = now
                )
            )
        }
    }

    @Test
    fun `fatal error - if loader throws an Error`(): Unit = runBlocking {
        val error = OutOfMemoryError()
        coEvery { loader.load(any()) } throws error
        val now = nowMillis()
        every { clock.instant() } returns now
        val loadTaskId = generateLoadTaskId()
        coEvery { loadTaskService.get(loadTaskId) } returns LoadTask(
            id = loadTaskId,
            type = testLoaderType,
            key = "key",
            attempts = 0, // Already the second (of two) retry attempt.
            scheduledAt = now,
            failedAt = null,
            retryAt = null
        )
        val loadTask = KafkaLoadTaskId(
            id = loadTaskId
        )
        assertThrows<OutOfMemoryError> { loadRunner.load(loadTask) }
        coVerify(exactly = 0) { loadTaskService.save(any()) }
    }
}
*/
