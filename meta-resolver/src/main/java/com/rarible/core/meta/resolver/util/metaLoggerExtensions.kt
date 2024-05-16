package com.rarible.core.meta.resolver.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("meta-provider")

fun <T> logMetaLoading(id: T, message: String, warn: Boolean = false) =
    logger.logMetaLoading(id.toString(), message, warn)

fun logMetaLoading(id: String, message: String, warn: Boolean = false) = logger.logMetaLoading(id, message, warn)

private fun Logger.logMetaLoading(id: String, message: String, warn: Boolean = false) {
    val logMessage = "Meta of $id: $message"
    if (warn) {
        this.warn(logMessage)
    } else {
        this.info(logMessage)
    }
}
