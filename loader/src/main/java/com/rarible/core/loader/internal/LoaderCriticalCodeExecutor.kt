package com.rarible.core.loader.internal

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.math.pow

@Component
class LoaderCriticalCodeExecutor(
    private val retryAttempts: Int = 5,
    private val backoffBaseDelay: Long = 1000
) {
    private val logger = LoggerFactory.getLogger("loader-global")

    suspend fun <T> retryOrFatal(actionName: String, block: suspend () -> T): T {
        val causes = arrayListOf<Throwable>()
        for (attempt in 0 until retryAttempts) {
            try {
                return block()
            } catch (e: Throwable) {
                causes += e
                val backoffDelay = exponentialBackoffDelayMillis(attempt)
                val needRetry = attempt < retryAttempts - 1
                logger.error(
                    buildString {
                        append("Failed attempt #${attempt + 1}/$retryAttempts of $actionName")
                        if (needRetry) {
                            append(", will retry in ${backoffDelay / 1000} seconds")
                        }
                    }, e
                )
                if (needRetry) {
                    delay(backoffDelay)
                }
            }
        }
        val cause = causes.last()
        val fatalError = LoadFatalError("Failed $retryAttempts attempts to $actionName", cause)
        causes.dropLast(1).forEach { fatalError.addSuppressed(it) }
        logger.error("Fatal error while running the $actionName, stopping the whole loader infrastructure", fatalError)
        throw fatalError
    }

    private fun exponentialBackoffDelayMillis(attempt: Int): Long =
        backoffBaseDelay * 2.0.pow(attempt.toDouble()).toLong()
}
