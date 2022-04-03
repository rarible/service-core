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

    private val receiveContentMetaTypeSuccessCounter =
        meterRegistry.counter("${PREFIX}_content_meta_type_success")

    private val receiveContentMetaTypeFailCounter =
        meterRegistry.counter("${PREFIX}_content_meta_type_fail")

    private val receiveContentMetaWidthHeightSuccessCounter =
        meterRegistry.counter("${PREFIX}_content_meta_width_height_success")

    private val receiveContentMetaWidthHeightFailCounter =
        meterRegistry.counter("${PREFIX}_content_meta_width_height_fail")

    private val receivedBytes =
        meterRegistry.counter("${PREFIX}_received_bytes")

    fun receiveContentMetaTypeSuccess() = receiveContentMetaTypeSuccessCounter.increment()

    fun receiveContentMetaTypeFail() = receiveContentMetaTypeFailCounter.increment()

    fun receiveContentMetaWidthHeightSuccess() = receiveContentMetaWidthHeightSuccessCounter.increment()

    fun receiveContentMetaWidthHeightFail() = receiveContentMetaWidthHeightFailCounter.increment()

    fun startReceiving(): Timer.Sample = Timer.start()

    fun endReceiving(sample: Timer.Sample, success: Boolean): Duration {
        receiveSuccessTimer.count()
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
}
