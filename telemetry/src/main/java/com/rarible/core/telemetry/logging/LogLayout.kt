package com.rarible.core.telemetry.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import com.batch.escalog.LogFmtLayout

class LogLayout : LogFmtLayout() {
    override fun doLayout(iLoggingEvent: ILoggingEvent?): String {
        val log = super.doLayout(iLoggingEvent)
        val extraLength = log.length - MAX_LENGTH
        return if (extraLength < 0) log else cut(log, iLoggingEvent, extraLength)
    }

    private fun cut(originalLog: String, iLoggingEvent: ILoggingEvent?, length: Int): String {
        if (iLoggingEvent == null) return originalLog

        val cutErrorLog = cutLogValue(
            originalLog,
            escapeValue(ThrowableProxyUtil.asString(iLoggingEvent.throwableProxy)).toString(),
            length
        )
        val extraLength = cutErrorLog.length - MAX_LENGTH

        return if (extraLength < 0)
            cutErrorLog
        else
            cutLogValue(cutErrorLog, escapeValue(iLoggingEvent.message).toString(), extraLength)
    }

    private fun cutLogValue(log: String, value: String?, length: Int): String {
        return if (!value.isNullOrEmpty()) log.replace(value, value.dropLast(length)) else log
    }

    companion object {
        private const val MAX_LENGTH = 16000
    }
}