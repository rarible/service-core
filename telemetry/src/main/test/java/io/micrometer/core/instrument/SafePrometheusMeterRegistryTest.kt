package io.micrometer.core.instrument

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class SafePrometheusMeterRegistryTest {

    private val registry = CollectorRegistry()
    private val prometheusMeterRegistry = PrometheusMeterRegistry(
        PrometheusConfig.DEFAULT,
        registry,
        Clock.SYSTEM,
        null
    )

    @Test
    fun `timer - ok`() {
        val meterRegistry = SafePrometheusMeterRegistry(prometheusMeterRegistry)
        val timer = meterRegistry.timer("test")
        timer.record(Duration.ofSeconds(1))

        // Timer successfully registered in registry
        val sample = registry.getSampleValue("test_seconds_count")
        assertThat(sample).isEqualTo(1.0)
    }
}
