package com.rarible.core.loader.internal.common

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Clock.nowMillis(): Instant = instant().truncatedTo(ChronoUnit.MILLIS)

fun Duration.presentable(): String = toMillis().toString() + " ms"
