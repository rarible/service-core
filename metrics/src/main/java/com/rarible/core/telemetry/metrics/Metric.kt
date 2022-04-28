package com.rarible.core.telemetry.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.time.Duration
import java.util.Locale

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

abstract class GaugeMetric protected constructor(name: String, vararg tags: Tag, private val valueSupplier: () -> Double): Metric(name, *tags) {
    fun bind(registry: MeterRegistry): RegisteredGauge {
        val gauge = Gauge
            .builder(name, valueSupplier)
            .tags(tags)
            .register(registry)
        return RegisteredGauge(gauge)
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

inline fun <T> MeterRegistry.measure(timingMetric: TimingMetric, block: () -> T): T {
    return timingMetric.bind(this).measure(block)
}

fun MeterRegistry.recordTime(timingMetric: TimingMetric, duration: Duration) {
    timingMetric.bind(this).record(duration)
}

fun MeterRegistry.recordValue(distributionSummary: DistributionSummaryMetric, amount: Number) {
    distributionSummary.bind(this).record(amount)
}

fun MeterRegistry.gauge(gaugeMetric: GaugeMetric) {
    gaugeMetric.bind(this).record()
}