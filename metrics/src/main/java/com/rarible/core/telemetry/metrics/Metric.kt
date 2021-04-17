package com.rarible.core.telemetry.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.util.*

abstract class Metric internal constructor(
    protected val name: String,
    vararg tags: Tag
) {
    protected val tags = tags.asList()

    companion object {
        fun tag(name: String, value: String): Tag = Tag.of(name, value)

        fun tag(name: String, value: Enum<*>): Tag = tag(name, value.name.toLowerCase(Locale.ROOT))
    }
}

abstract class CountingMetric protected constructor(name: String, vararg tags: Tag) : Metric(name, *tags) {
    fun bind(registry: MeterRegistry): RegisteredCounter {
        val counter = registry.counter(name, tags)
        return RegisteredCounter(counter)
    }
}

abstract class TimingMetric protected constructor(name: String, vararg tags: Tag) : Metric(name, *tags) {
    fun bind(registry: MeterRegistry): RegisteredTimer {
        val timer = registry.timer(name, tags)
        return RegisteredTimer(timer, registry.config().clock())
    }
}

fun MeterRegistry.increment(countingMetric: CountingMetric, size: Number = 1) {
    countingMetric.bind(this).increment(size)
}

inline fun <T> MeterRegistry.measure(timingMetric: TimingMetric, block: () -> T): T {
    return timingMetric.bind(this).measure(block)
}