package com.rarible.core.daemon

import com.rarible.core.telemetry.metrics.CountingMetric

class DaemonIncome(workerName: String) : CountingMetric(
    workerName,
    tag("task", "income")
)

class DaemonLiveness(workerName: String) : CountingMetric(
    workerName,
    tag("live", "point")
)

class DaemonError(workerName: String) : CountingMetric(
    workerName,
    tag("error", "execution_error")
)

class DaemonProcessingError(workerName: String) : CountingMetric(
    workerName,
    tag("error", "processing_error")
)

class DaemonClosedChannelEvent(workerName: String) : CountingMetric(
    workerName,
    tag("event", "closed_channel")
)
