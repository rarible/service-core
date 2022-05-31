package com.rarible.core.content.meta.loader

import com.rarible.core.meta.resource.detector.ContentBytes
import io.micrometer.core.instrument.MeterRegistry
import java.io.Closeable
import java.net.URL

class MeasurableContentReceiver(
    private val delegate: ContentReceiver,
    meterRegistry: MeterRegistry
) : ContentReceiver, Closeable {
    private val receiveCallCounter = meterRegistry.counter("${PREFIX}_receiver_success")
    private val receiveErrorCounter = meterRegistry.counter("${PREFIX}_receiver_error")

    override suspend fun receiveBytes(url: URL, maxBytes: Int): ContentBytes {
        return try {
            delegate.receiveBytes(url, maxBytes).also {
                receiveCallCounter.increment()
            }
        } catch (ex: Throwable) {
            receiveErrorCounter.increment()
            throw ex
        }
    }

    override fun close() {
        delegate.close()
    }
}
