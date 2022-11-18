package com.rarible.core.telemetry.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.contrib.json.classic.JsonLayout

/**
 * Customized JSON Layout makes JSON log format compatible with existing logs in ELK
 * since uses same field names as in current LogFmt format.
 */
class JsonLogLayout : JsonLayout() {

    override fun toJsonMap(event: ILoggingEvent): Map<*, *> {
        val map: Map<String, Any> = TrimmedLinkedHashMap()
        add("level", includeLevel, event.level.toString().lowercase(), map)
        add("thread", includeThreadName, event.threadName, map)
        add("msg", includeFormattedMessage, event.formattedMessage, map)
        addMap("mdc", includeMDC, event.mdcPropertyMap, map)
        addPackageAndClassName(event, map)
        addThrowableInfo("error", includeException, event, map)
        return map
    }

    // Works only if we're using full class names in Logger name
    private fun addPackageAndClassName(iLoggingEvent: ILoggingEvent, map: Map<String, Any>) {
        val className = iLoggingEvent.loggerName
        val lastPointPosition = className.lastIndexOf('.')
        if (lastPointPosition < 0) {
            add("module", includeLoggerName, className, map)
            add("package", includeLoggerName, "", map)
        } else {
            add("module", includeLoggerName, className.substring(lastPointPosition + 1), map)
            add("package", includeLoggerName, className.substring(0, lastPointPosition), map)
        }
    }

    class TrimmedLinkedHashMap : LinkedHashMap<String, Any>() {

        override fun put(key: String, value: Any): Any? {
            return if (value is String && value.length > MAX_LENGTH) {
                super.put(key, value.substring(0, MAX_LENGTH))
            } else {
                super.put(key, value)
            }
        }
    }

    companion object {

        private const val MAX_LENGTH = 16000
    }
}