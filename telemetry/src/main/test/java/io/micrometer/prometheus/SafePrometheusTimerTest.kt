package io.micrometer.prometheus

import io.micrometer.core.instrument.Clock
import io.prometheus.client.CollectorRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

class SafePrometheusTimerTest {

    private val safePrometheusMeterRegistry = SafePrometheusMeterRegistry(
        PrometheusConfig.DEFAULT,
        CollectorRegistry(),
        Clock.SYSTEM,
        null
    )

    private val precision = Percentage.withPercentage(0.00001)

    @Test
    fun `record - ok, small values`() {
        val timer = safePrometheusMeterRegistry.timer("small")
        timer.record(100, TimeUnit.NANOSECONDS)
        timer.record(50, TimeUnit.NANOSECONDS)

        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isCloseTo(150.0, precision)

        timer.record(1, TimeUnit.MINUTES)

        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isCloseTo(60_000_000_150.0, precision)
    }

    @Test
    fun `record - ok, big values`() {
        val timer = safePrometheusMeterRegistry.timer("big")
        var total = 0.0
        val nanos = Duration.ofDays(100).toMillis() * 1000_1000
        while (total < Long.MAX_VALUE) {
            total += nanos
            timer.record(nanos, TimeUnit.NANOSECONDS)
        }

        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isCloseTo(9.24572448E18, precision)
    }
}
