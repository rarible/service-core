package com.rarible.core.content.meta.loader

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import java.time.Duration

class ContentReceiverMetrics(
    meterRegistry: MeterRegistry
) {

    private val receiveSuccessTimer =
        meterRegistry.timer(
            "${PREFIX}_time",
            listOf(Tag.of("success", "true"))
        )

    private val receiveFailureTimer =
        meterRegistry.timer(
            "${PREFIX}_time",
            listOf(Tag.of("success", "false"))
        )

    private val receivedBytes = meterRegistry.counter("${PREFIX}_received_bytes")

    fun startReceiving(): Timer.Sample = Timer.start()

    fun endReceiving(sample: Timer.Sample, success: Boolean): Duration {
        val time = if (success) {
            sample.stop(receiveSuccessTimer)
        } else {
            sample.stop(receiveFailureTimer)
        }
        return Duration.ofNanos(time)
    }

    fun receivedBytes(count: Int) {
        receivedBytes.increment(count.toDouble())
    }

    val totalBytesReceived: Long get() = receivedBytes.count().toLong()

    private fun Boolean.booleanToString() = if (this) "true" else "false"

    private companion object {
        const val PREFIX = "content_meta_loader"
    }

}
