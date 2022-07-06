package com.rarible.core.telemetry.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import com.batch.escalog.LogFmtLayout

class LogLayout : LogFmtLayout() {
    override fun doLayout(iLoggingEvent: ILoggingEvent?): String {
        val log = super.doLayout(iLoggingEvent)
        return if (log.length < MAX_LENGTH) log else (log.take(MAX_LENGTH - 1).plus("\n"))
    }

    companion object {
        private const val MAX_LENGTH = 16000
    }
}