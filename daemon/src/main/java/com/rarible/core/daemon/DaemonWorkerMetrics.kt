package com.rarible.core.daemon

import com.rarible.core.telemetry.metrics.CountingMetric

private const val METRIC_NAME = "worker_name"
private const val WORKER_TAG_NAME = "worker"
private const val EVENT_TAG_NAME = "event"

class DaemonIncome(workerName: String) : CountingMetric(
    METRIC_NAME,
    tag(WORKER_TAG_NAME, workerName),
    tag(EVENT_TAG_NAME, "income")
)

class DaemonLiveness(workerName: String) : CountingMetric(
    METRIC_NAME,
    tag(WORKER_TAG_NAME, workerName),
    tag(EVENT_TAG_NAME, "alive")
)

class DaemonError(workerName: String) : CountingMetric(
    METRIC_NAME,
    tag(WORKER_TAG_NAME, workerName),
    tag(EVENT_TAG_NAME, "execution_error")
)

class DaemonProcessingError(workerName: String) : CountingMetric(
    METRIC_NAME,
    tag(WORKER_TAG_NAME, workerName),
    tag(EVENT_TAG_NAME, "processing_error")
)

class DaemonClosedChannelEvent(workerName: String) : CountingMetric(
    METRIC_NAME,
    tag("worker", workerName),
    tag("event", "closed_channel")
)
