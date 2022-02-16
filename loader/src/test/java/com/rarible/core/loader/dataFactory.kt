package com.rarible.core.loader

import com.rarible.core.common.nowMillis
import com.rarible.core.loader.internal.common.LoadTask
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import java.time.Instant
import java.util.concurrent.TimeUnit

fun randomLoadTask(
    type: LoadType = randomString(),
    status: LoadTask.Status,
) = LoadTask(
    id = generateLoadTaskId(),
    type = type,
    key = randomString(),
    status = status
)

fun randomScheduledStatus() = LoadTask.Status.Scheduled(
    scheduledAt = randomInstant(),
)

fun randomLoadedStatus() = LoadTask.Status.Loaded(
    scheduledAt = randomInstant(),
    retryAttempts = randomInt(),
    loadedAt = randomInstant()
)

fun randomWaitsForRetryStatus() = LoadTask.Status.WaitsForRetry(
    scheduledAt = randomInstant(),
    retryAttempts = randomInt(),
    retryAt = randomInstant(),
    failedAt = randomInstant(),
    errorMessage = randomString(),
    rescheduled = randomBoolean()
)

fun randomFailedStatus() = LoadTask.Status.Failed(
    scheduledAt = randomInstant(),
    retryAttempts = randomInt(),
    failedAt = randomInstant(),
    errorMessage = randomString()
)

fun randomInstant(): Instant {
    val now = nowMillis()
    val minInstant = now.minusMillis(TimeUnit.DAYS.toMillis(10))
    val maxInstant = now.plusMillis(TimeUnit.DAYS.toMillis(10))
    return Instant.ofEpochSecond(randomLong(minInstant.epochSecond, maxInstant.epochSecond))
}
