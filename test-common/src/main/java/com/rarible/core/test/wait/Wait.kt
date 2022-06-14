package com.rarible.core.test.wait

import kotlinx.coroutines.time.delay
import java.time.Duration

object Wait {
    suspend fun <V> waitFor(
        timeout: Duration = Duration.ofSeconds(5),
        callable: suspend () -> V
    ): V {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout.toMillis()) {
            try {
                val value = callable.invoke()
                if (value != null) {
                    return value
                }
                delay(Duration.ofMillis(500))
            } catch (ignored: Exception) {
            }
        }
        throw AssertionError("Failed wait $callable")
    }

    suspend fun waitAssert(
        timeout: Duration = Duration.ofSeconds(5),
        runnable: suspend () -> Unit
    ) {
        return waitAssertWithCheckInterval(
            checkInterval = Duration.ofMillis(500),
            timeout = timeout,
            runnable = runnable
        )
    }

    suspend fun waitAssertWithCheckInterval(
        checkInterval: Duration = Duration.ofMillis(500),
        timeout: Duration = Duration.ofSeconds(5),
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