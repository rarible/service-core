package com.rarible.core.telemetry.metrics

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import com.rarible.core.telemetry.metrics.configuration.InfraNamingConventionFactoring
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.prometheus.SafePrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exemplars.ExemplarSampler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfigureBefore(
    CompositeMeterRegistryAutoConfiguration::class,
    SimpleMetricsExportAutoConfiguration::class,
    PrometheusMetricsExportAutoConfiguration::class
)
@EnableConfigurationProperties(
    MetricsNamingProperties::class
)
class MetricsAutoConfiguration(
    private val appInfo: ApplicationInfo,
    private val envInfo: ApplicationEnvironmentInfo
) {
    @Bean
    fun metricNameCustomizer(): MeterRegistryCustomizer<*> {
        val commonTags = InfraNamingConventionFactoring.getCommonTags(appInfo, envInfo)

        return MeterRegistryCustomizer<MeterRegistry> { registry ->
            with(registry.config()) {
                commonTags(commonTags)
            }
        }
    }

    @Bean
    fun safePrometheusMeterRegistry(
        prometheusConfig: PrometheusConfig,
        collectorRegistry: CollectorRegistry,
        clock: Clock,
        exemplarSamplerProvider: ObjectProvider<ExemplarSampler>
    ): PrometheusMeterRegistry {
        return SafePrometheusMeterRegistry(
            prometheusConfig,
            collectorRegistry,
            clock,
            exemplarSamplerProvider.getIfAvailable()
        )
    }
}
