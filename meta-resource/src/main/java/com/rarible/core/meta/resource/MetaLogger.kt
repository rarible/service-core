package com.rarible.core.meta.resource

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MetaLogger {

    private val itemMetaLogger: Logger = LoggerFactory.getLogger("item-meta-loading")

    fun logMetaLoading(id: String, message: String, warn: Boolean = false) = itemMetaLogger.logMetaLoading(id, message, warn)

    fun Logger.logMetaLoading(id: String, message: String, warn: Boolean = false) {
        val logMessage = "Meta of $id: $message"
        if (warn) {
            this.warn(logMessage)
        } else {
            this.info(logMessage)
        }
    }
}
