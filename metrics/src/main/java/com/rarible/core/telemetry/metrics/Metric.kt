package com.rarible.core.telemetry.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.time.Duration
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

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

abstract class LongGaugeMetric protected constructor(name: String, vararg tags: Tag) : Metric(name, *tags) {
    fun bind(registry: MeterRegistry): RegisteredGauge<Long> {
        val number = requireNotNull(registry.gauge(name, tags, AtomicLong(0)))
        return RegisteredGauge(object : GaugeModifier<Long> {
            override fun set(value: Long) {
                number.set(value)
            }
        })
    }
}

abstract class TimingMetric protected constructor(name: String, vararg tags: Tag) : Metric(name, *tags) {
    fun bind(registry: MeterRegistry): RegisteredTimer {
        val timer = registry.timer(name, tags)
        return RegisteredTimer(timer, registry.config().clock())
    }
}

abstract class DistributionSummaryMetric protected constructor(name: String, vararg tags: Tag) : Metric(name, *tags) {
    fun bind(registry: MeterRegistry): RegisteredDistributionSummary {
        val distributionSummary = registry.summary(name, tags)
        return RegisteredDistributionSummary(distributionSummary)
    }
}

fun MeterRegistry.increment(countingMetric: CountingMetric, size: Number = 1) {
    countingMetric.bind(this).increment(size)
}

interface GaugeModifier<T : Number> {
    fun set(value: T)
}

inline fun <T> MeterRegistry.measure(timingMetric: TimingMetric, block: () -> T): T {
    return timingMetric.bind(this).measure(block)
}

fun MeterRegistry.recordTime(timingMetric: TimingMetric, duration: Duration) {
    timingMetric.bind(this).record(duration)
}

fun MeterRegistry.recordValue(distributionSummary: DistributionSummaryMetric, amount: Number) {
    distributionSummary.bind(this).record(amount)
}