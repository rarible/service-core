package com.rarible.core.apm

object SpanType {
    // Default types of APM
    const val DB: String = "db"
    const val APP: String = "app"
    const val EXT: String = "ext"
    const val CACHE: String = "cache"
    const val TEMPLATE: String = "template"

    // Custom types
    const val KAFKA: String = "kafka" // To measure kafka interactions (publicEvent, for example)
    const val EVENT: String = "event" // To measure event handling operations
}
