package com.rarible.core.test.wait

import kotlinx.coroutines.time.delay
import java.time.Duration

object Wait {

    private val defaultInterval = Duration.ofMillis(100)
    private val isDebugging = System.getProperty("debug") == "true"
    private val defaultDuration = if (isDebugging) Duration.ofDays(30) else Duration.ofSeconds(5)

    suspend fun <V> waitFor(
        timeout: Duration = defaultDuration,
        callable: suspend () -> V
    ): V {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout.toMillis()) {
            try {
                val value = callable.invoke()
                if (value != null) {
                    return value
                }
                delay(defaultInterval)
            } catch (ignored: Exception) {
            }
        }
        throw AssertionError("Failed wait $callable")
    }

    suspend fun waitAssert(
        timeout: Duration = defaultDuration,
        runnable: suspend () -> Unit
    ) {
        return waitAssertWithCheckInterval(
            checkInterval = defaultInterval,
            timeout = timeout,
            runnable = runnable
        )
    }

    suspend fun waitAssertWithCheckInterval(
        checkInterval: Duration = defaultInterval,
        timeout: Duration = defaultDuration,
        runnable: suspend () -> Unit
    ) {
        val maxTime = System.currentTimeMillis() + timeout.toMillis()
        while (true) {
            try {
                runnable.invoke()
                return
            } catch (e: Throwable) {
                when (e) {
                    is AssertionError, is KotlinNullPointerException -> {
                        if (System.currentTimeMillis() > maxTime) {
                            throw e
                        }
                        try {
                            delay(checkInterval)
                        } catch (ignore: InterruptedException) {
                        }
                    }
                    else -> throw e
                }
            }
        }
    }
}
