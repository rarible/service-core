package com.rarible.core.telemetry.metrics

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import com.rarible.core.telemetry.metrics.configuration.InfraNamingConventionFactoring
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.SafePrometheusMeterRegistry
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

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

    @Primary
    @Bean
    fun safePrometheusMeterRegistry(prometheusMeterRegistry: PrometheusMeterRegistry): MeterRegistry {
        return SafePrometheusMeterRegistry(prometheusMeterRegistry)
    }
}
