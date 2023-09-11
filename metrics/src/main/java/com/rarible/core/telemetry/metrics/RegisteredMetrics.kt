package com.rarible.core.telemetry.metrics

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.TimeUnit

class RegisteredCounter internal constructor(
    private val counter: Counter
) {
    fun increment(number: Number = 1): Unit = counter.increment(number.toDouble())
}

class RegisteredGauge<T : Number> internal constructor(
    private val gaugeModifier: GaugeModifier<T>
) {
    fun set(number: T): Unit = gaugeModifier.set(number)
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

    fun record(duration: Duration) {
        timer.record(duration)
    }

    fun record(amount: Long, unit: TimeUnit) {
        timer.record(amount, unit)
    }
}

class RegisteredDistributionSummary internal constructor(
    private val distributionSummary: DistributionSummary
) {
    fun record(amount: Number) {
        distributionSummary.record(amount.toDouble())
    }
}
