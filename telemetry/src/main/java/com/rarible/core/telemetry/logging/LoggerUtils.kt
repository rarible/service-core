package com.rarible.core.telemetry.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory

internal fun getLogbackLoggerContext(): LoggerContext {
    return LoggerFactory.getILoggerFactory() as LoggerContext
}

internal fun LoggerContext.getRootLogger() = getLogger(Logger.ROOT_LOGGER_NAME)
