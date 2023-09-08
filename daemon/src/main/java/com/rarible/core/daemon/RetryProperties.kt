package com.rarible.core.daemon

import java.time.Duration

data class RetryProperties(
    val attempts: Int = 3,
    val delay: Duration = Duration.ZERO
)
