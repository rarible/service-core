package com.rarible.core.common

import java.time.Instant
import java.time.temporal.ChronoUnit

fun nowMillis(): Instant {
    return Instant.now().truncatedTo(ChronoUnit.MILLIS)
}