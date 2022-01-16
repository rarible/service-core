package com.rarible.core.daemon

import java.time.Duration

data class DaemonWorkerProperties(
    val pollingPeriod: Duration = Duration.ofSeconds(30),
    val errorDelay: Duration = Duration.ofSeconds(60),
    val consumerBatchSize: Int = 512
)
