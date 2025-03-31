package com.rarible.core.meta.resource.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

class ResponseSizeRecorder(
    private val monitoredUrls: List<String>,
    private val meterRegistry: MeterRegistry,
) {
    fun recordResponseSize(blockchain: String, url: String, responseSize: Int) {
        val monitoredUrl = monitoredUrls.firstOrNull { url.startsWith(it) } ?: return
        meterRegistry.counter(
            METRIC_NAME,
            listOf(Tag.of("url", monitoredUrl), Tag.of("blockchain", blockchain))
        ).increment(responseSize.toDouble())
    }

    companion object {
        const val METRIC_NAME = "protocol_external_response_size"
    }
}
