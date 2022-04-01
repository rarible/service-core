package com.rarible.core.content.meta.loader

import io.micrometer.core.instrument.MeterRegistry
import java.io.Closeable
import java.net.URL

class MeasurableContentReceiver(
    private val delegate: ContentReceiver,
    meterRegistry: MeterRegistry
) : Closeable {
    private val receiveCallCounter = meterRegistry.counter("${PREFIX}_receiver_call")
    private val receiveErrorCounter = meterRegistry.counter("${PREFIX}_receiver_error")

    suspend fun receiveBytes(url: URL, maxBytes: Int): ContentBytes {
        return try {
            delegate.receiveBytes(url, maxBytes)
        } catch (ex: Throwable) {
            receiveErrorCounter.increment()
            throw ex
        } finally {
            receiveCallCounter.increment()
        }
    }

    override fun close() {
        delegate.close()
    }
}