package com.rarible.core.common

import java.time.Instant
import java.time.temporal.ChronoField

fun nowMillis(): Instant {
    return Instant.now().with(ChronoField.NANO_OF_SECOND, 0)
}