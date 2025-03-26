package com.rarible.core.meta.resource.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MetaLogger {

    private val itemMetaLogger: Logger = LoggerFactory.getLogger("item-meta-loading")

    fun <T> logMetaLoading(id: T, message: String, warn: Boolean = false, exception: Throwable? = null) {
        itemMetaLogger.logMetaLoading(id.toString(), message, warn, exception)
    }

    fun Logger.logMetaLoading(id: String, message: String, warn: Boolean = false, exception: Throwable? = null) {
        val logMessage = "Meta of $id: $message"
        if (warn) {
            this.warn(logMessage, exception)
        } else {
            this.info(logMessage, exception)
        }
    }
}
