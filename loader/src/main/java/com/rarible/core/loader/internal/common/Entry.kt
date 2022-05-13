package com.rarible.core.loader.internal.common

import java.time.Instant

class Entry<T>(
    val version: Long,
    val id: String,
    val status: Status,
    val data: T?,
    val attempts: Int,
    // When task scheduled last time, should not affect updatedAt
    val scheduledAt: Instant,
    // When entry updated last time, should be changed only on success/fail
    val updatedAt: Instant?,
    // When was the last successful load/refresh
    val succeedAt: Instant?,
    // When was the last fail
    val failedAt: Instant?
)

enum class Status {
    SCHEDULED,  // Never loaded, will be retried soon
    SUCCESS,    // Successfully downloaded
    RETRY,      // Waits for retry
    FAILED      // Completely failed
}

fun main() {
    val a = Entry<String>("a")
    a.data
    a.scheduledAt
    a.updatedAt
    a.version
    a.attempts
    a.failedAt
    a.succeedAt
}