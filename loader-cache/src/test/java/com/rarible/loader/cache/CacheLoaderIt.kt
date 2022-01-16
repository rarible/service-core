package com.rarible.loader.cache

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.LoadTaskStatus
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.loader.cache.test.TestImage
import com.rarible.loader.cache.test.cacheEvents
import com.rarible.loader.cache.test.testCacheType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CacheLoaderIt : AbstractIntegrationTest() {
    @Test
    fun load() = runBlocking<Unit> {
        val key = randomString()
        val testImage = TestImage("content")
        val scheduledAt = nowMillis()
        every { clock.instant() } returns scheduledAt
        val imageChannel = Channel<TestImage>()
        coEvery { imageLoader.load(key) } coAnswers { imageChannel.receive() }
        assertThat(imageLoadService.get(key)).isEqualTo(CacheEntry.NotAvailable<TestImage>())
        imageLoadService.update(key)
        val initialLoadScheduledCacheEntry = CacheEntry.InitialLoadScheduled<TestImage>(
            loadStatus = LoadTaskStatus.Scheduled(
                scheduledAt = scheduledAt
            )
        )
        assertThat(imageLoadService.get(key)).isEqualTo(initialLoadScheduledCacheEntry)
        Wait.waitAssert {
            assertThat(cacheEvents).isEqualTo(
                listOf(
                    CacheLoaderEvent(
                        type = testCacheType,
                        key = key,
                        cacheEntry = initialLoadScheduledCacheEntry
                    )
                )
            )
        }
        cacheEvents.clear()

        // Trigger the loader.
        val loadedAt = scheduledAt.plusSeconds(1)
        every { clock.instant() } returns loadedAt
        imageChannel.send(testImage)

        Wait.waitAssert {
            val loadedEntry = CacheEntry.Loaded(
                cachedAt = loadedAt,
                data = testImage
            )
            assertThat(imageLoadService.get(key)).isEqualTo(loadedEntry)
            assertThat(imageLoadService.getAvailable(key)).isEqualTo(testImage)
            assertThat(cacheEvents).isEqualTo(
                listOf(
                    CacheLoaderEvent(
                        type = testCacheType,
                        key = key,
                        cacheEntry = loadedEntry
                    )
                )
            )
        }
    }

    @Test
    fun `update cached entry`() = runBlocking<Unit> {
        val key = randomString()
        val testImage = TestImage("content")
        val imageChannel = Channel<TestImage>()
        coEvery { imageLoader.load(key) } coAnswers { imageChannel.receive() }
        imageLoadService.update(key)

        val scheduledAt = nowMillis()
        every { clock.instant() } returns scheduledAt

        // Trigger the loader.
        val loadedAt = scheduledAt.plusSeconds(1)
        every { clock.instant() } returns loadedAt
        imageChannel.send(testImage)

        Wait.waitAssert {
            val loadedEntry = CacheEntry.Loaded(
                cachedAt = loadedAt,
                data = testImage
            )
            assertThat(imageLoadService.get(key)).isEqualTo(loadedEntry)
            assertThat(imageLoadService.getAvailable(key)).isEqualTo(testImage)
        }

        val updateScheduledAt = loadedAt.plusSeconds(1)
        every { clock.instant() } returns updateScheduledAt

        cacheEvents.clear()
        imageLoadService.update(key)
        Wait.waitAssert {
            val loadedAndUpdateScheduled = CacheEntry.LoadedAndUpdateScheduled(
                cachedAt = loadedAt,
                data = testImage,
                updateStatus = LoadTaskStatus.Scheduled(
                    scheduledAt = updateScheduledAt
                )
            )
            assertThat(imageLoadService.get(key)).isEqualTo(loadedAndUpdateScheduled)
            assertThat(imageLoadService.getAvailable(key)).isEqualTo(testImage)
            assertThat(cacheEvents).isEqualTo(
                listOf(
                    CacheLoaderEvent(
                        type = testCacheType,
                        key = key,
                        cacheEntry = loadedAndUpdateScheduled
                    )
                )
            )
        }

        // Trigger the update.
        cacheEvents.clear()
        val loadedAt2 = updateScheduledAt.plusSeconds(1)
        every { clock.instant() } returns loadedAt2
        val testImage2 = testImage.copy(content = "content2")
        imageChannel.send(testImage2)

        Wait.waitAssert {
            val loadedEntry = CacheEntry.Loaded(
                cachedAt = loadedAt2,
                data = testImage2
            )
            assertThat(imageLoadService.get(key)).isEqualTo(loadedEntry)
            assertThat(imageLoadService.getAvailable(key)).isEqualTo(testImage2)
            assertThat(cacheEvents).isEqualTo(
                listOf(
                    CacheLoaderEvent(
                        type = testCacheType,
                        key = key,
                        cacheEntry = loadedEntry
                    )
                )
            )
        }
    }

    @Test
    fun remove() = runBlocking<Unit> {
        val key = randomString()
        val testImage = TestImage("content")

        val imageChannel = Channel<TestImage>()
        coEvery { imageLoader.load(key) } coAnswers { imageChannel.receive() }

        val scheduledAt = nowMillis()
        every { clock.instant() } returns scheduledAt

        imageLoadService.update(key)

        // Trigger the loader.
        val loadedAt = scheduledAt.plusSeconds(1)
        every { clock.instant() } returns loadedAt
        imageChannel.send(testImage)

        Wait.waitAssert {
            val loadedEntry = CacheEntry.Loaded(
                cachedAt = loadedAt,
                data = testImage
            )
            assertThat(imageLoadService.get(key)).isEqualTo(loadedEntry)
            assertThat(imageLoadService.getAvailable(key)).isEqualTo(testImage)
            assertThat(cacheEvents).isNotEmpty
        }

        cacheEvents.clear()
        imageLoadService.remove(key)
        Wait.waitAssert {
            val notAvailable = CacheEntry.NotAvailable<TestImage>()
            assertThat(imageLoadService.get(key)).isEqualTo(notAvailable)
            assertThat(imageLoadService.getAvailable(key)).isNull()
        }
    }

    @Test
    fun `retry if an error occurs`() = runBlocking<Unit> {
        val key = randomString()
        val imageChannel = Channel<Result<TestImage>>()
        coEvery { imageLoader.load(key) } coAnswers {
            val result = imageChannel.receive()
            result.getOrThrow()
        }

        val scheduledAt = nowMillis()
        every { clock.instant() } returns scheduledAt
        imageLoadService.update(key)

        Wait.waitAssert {
            assertThat(cacheEvents).hasSize(1)
        }

        // Trigger the loader with exception.
        cacheEvents.clear()
        val exceptionAt = scheduledAt.plusSeconds(1)
        every { clock.instant() } returns exceptionAt
        val error = RuntimeException("error")
        imageChannel.send(Result.failure(error))

        Wait.waitAssert {
            val waitsForRetry = LoadTaskStatus.WaitsForRetry(
                scheduledAt = scheduledAt,
                retryAttempts = 0,
                failedAt = exceptionAt,
                errorMessage = error.localizedMessage,
                retryAt = exceptionAt + loadProperties.retry.getRetryDelay(0)
            )
            val initialLoadScheduled = CacheEntry.InitialLoadScheduled<TestImage>(
                loadStatus = waitsForRetry
            )
            assertThat(imageLoadService.get(key)).isEqualTo(initialLoadScheduled)
            assertThat(cacheEvents).isEqualTo(
                listOf(
                    CacheLoaderEvent(
                        type = testCacheType,
                        key = key,
                        cacheEntry = initialLoadScheduled
                    )
                )
            )
        }

        val retryScheduleTime = exceptionAt.plusSeconds(1)
        every { clock.instant() } returns retryScheduleTime

        retryTasksService.scheduleTasksToRetry()

        // Trigger the successful loading.
        cacheEvents.clear()
        val loadedAt = retryScheduleTime.plusSeconds(1)
        every { clock.instant() } returns loadedAt
        val testImage = TestImage("content")
        imageChannel.send(Result.success(testImage))

        Wait.waitAssert {
            val loadedEntry = CacheEntry.Loaded(
                cachedAt = loadedAt,
                data = testImage
            )
            assertThat(imageLoadService.get(key)).isEqualTo(loadedEntry)
            assertThat(cacheEvents).isEqualTo(
                listOf(
                    CacheLoaderEvent(
                        type = testCacheType,
                        key = key,
                        cacheEntry = loadedEntry
                    )
                )
            )
        }
    }

    @Test
    fun `get available - schedule loading if not available`() = runBlocking<Unit> {
        val key = randomString()
        val image = TestImage(randomString())
        coEvery { imageLoader.load(key) } returns image
        assertThat(imageLoadService.getAvailable(key)).isNull()
        imageLoadService.update(key)
        Wait.waitAssert {
            assertThat(imageLoadService.getAvailable(key)).isEqualTo(image)
            coVerify(exactly = 1) { imageLoader.load(key) }
        }
    }

    @Test
    fun `get available - update`() = runBlocking<Unit> {
        val key = randomString()
        val testImage = TestImage(randomString())
        coEvery { imageLoader.load(key) } returns testImage
        imageLoadService.update(key)
        Wait.waitAssert {
            assertThat(imageLoadService.getAvailable(key)).isEqualTo(testImage)
            coVerify(exactly = 1) { imageLoader.load(key) }
        }
    }
}
