package com.rarible.core.telemetry.metrics

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer

class RegisteredCounter internal constructor(
    private val counter: Counter
) {
    fun increment(number: Number = 1): Unit = counter.increment(number.toDouble())
}

class RegisteredTimer internal constructor(
    private val timer: Timer,
    private val clock: Clock
) {
    inline fun <R> measure(block: () -> R): R {
        val sample = startSample()
        try {
            return block()
        } finally {
            endSample(sample)
        }
    }

    fun startSample(): Timer.Sample {
        return Timer.start(clock)
    }

    fun endSample(sample: Timer.Sample) {
        sample.stop(timer)
    }
}