package com.rarible.core.test.wait

import org.junit.jupiter.api.Assertions
import org.opentest4j.AssertionFailedError

object BlockingWait {

    fun <V> waitFor(timeout: Long = 5000, callable: () -> V): V? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            try {
                val value = callable.invoke()
                if (value != null) {
                    return value
                }
                Thread.sleep(500)
            } catch (ignored: Exception) {
            }
        }
        Assertions.fail<Any>("Failed wait $callable")
        return null
    }

    @Throws(Exception::class)
    @JvmStatic
    fun waitAssert(timeout: Long = 5000, runnable: () -> Unit) {
        val maxTime = System.currentTimeMillis() + timeout
        while (true) {
            try {
                runnable.invoke()
                return
            } catch (e: Throwable) {
                when (e) {
                    is AssertionFailedError, is KotlinNullPointerException, is AssertionError, is NullPointerException -> {
                        if (System.currentTimeMillis() > maxTime) {
                            throw e
                        }
                        try {
                            Thread.sleep(500)
                        } catch (ignore: InterruptedException) {
                        }
                    }
                    else -> throw e
                }
            }
        }
    }
}