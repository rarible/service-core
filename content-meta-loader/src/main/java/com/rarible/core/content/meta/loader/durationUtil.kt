package com.rarible.core.content.meta.loader

import java.time.Duration

fun Duration.presentable(): String = toMillis().toString() + " ms"

fun Duration.presentableSlow(slowThreshold: Duration): String =
    toMillis().toString() + " ms" + if (this > slowThreshold) " (slow)" else ""
