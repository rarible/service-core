package com.rarible.core.telemetry.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import com.batch.escalog.LogFmtLayout

class LogLayout : LogFmtLayout() {
    override fun doLayout(iLoggingEvent: ILoggingEvent?): String {
        val log = super.doLayout(iLoggingEvent)
        val extraLength = log.length - MAX_LENGTH
        return if (extraLength < 0) log else cut(log, extraLength)
    }

    private fun cut(origLog: String, length: Int): String {
        val cutLog = cutLogByTab(origLog, ERROR_TAG, length)
        val extraLength = cutLog.length - MAX_LENGTH
        return if (extraLength < 0) cutLog else cutLogByTab(cutLog, MSG_TAG, extraLength)
    }

    private fun cutLogByTab(log: String, tag: String, length: Int): String {
        val value = getValue(log, tag)
        return if (value != null) log.replace(value, value.dropLast(length)) else log
    }

    private fun getValue(log: String, tag: String): String? {
        val index = log.indexOf(tag)
        if (index > 0) {
            val messageStartIndex = index + tag.length
            return if (log.getOrNull(messageStartIndex) == '"') {
                log.substring(messageStartIndex + 1, log.indexOf('"', messageStartIndex + 1))
            } else {
                log.substring(messageStartIndex, log.indexOf(' ', messageStartIndex))
            }
        } else {
            return null
        }
    }

    companion object {
        private const val MAX_LENGTH = 16000
        private const val MSG_TAG = "msg="
        private const val ERROR_TAG = "error="
    }
}