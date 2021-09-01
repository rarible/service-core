package com.rarible.core.daemon

import com.rarible.core.telemetry.metrics.CountingMetric

class DaemonIncome(workerName: String) : CountingMetric(
    workerName,
    tag("event", "income")
)

class DaemonLiveness(workerName: String) : CountingMetric(
    workerName,
    tag("event", "alive")
)

class DaemonError(workerName: String) : CountingMetric(
    workerName,
    tag("event", "execution_error")
)

class DaemonProcessingError(workerName: String) : CountingMetric(
    workerName,
    tag("event", "processing_error")
)

class DaemonClosedChannelEvent(workerName: String) : CountingMetric(
    workerName,
    tag("event", "closed_channel")
)
