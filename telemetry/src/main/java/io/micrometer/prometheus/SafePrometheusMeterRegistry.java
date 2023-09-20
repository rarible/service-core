// Do not change, delegate calls have PROTECTED visibility
package io.micrometer.prometheus;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exemplars.Exemplar;
import io.prometheus.client.exemplars.ExemplarSampler;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Delegation of protected method calls works only from Java classes from same package as MeterRegistry
@SuppressWarnings("ALL")
public class SafePrometheusMeterRegistry extends PrometheusMeterRegistry {

    private final PrometheusConfig prometheusConfig;
    private final ExemplarSampler exemplarSampler;

    public SafePrometheusMeterRegistry(
            PrometheusConfig config,
            CollectorRegistry registry,
            Clock clock,
            @Nullable
            ExemplarSampler exemplarSampler
    ) {
        super(config, registry, clock, exemplarSampler);
        this.prometheusConfig = config;
        this.exemplarSampler = exemplarSampler;
    }

    @Override
    public Timer newTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, PauseDetector pauseDetector) {
        SafePrometheusTimer timer = new SafePrometheusTimer(
                id,
                clock,
                distributionStatisticConfig,
                pauseDetector,
                prometheusConfig.histogramFlavor(), exemplarSampler
        );
        // It will be called only once when Timer created, unfortunately, reflection is the only way to push
        // this timer to the registry
        privateCall(
                "applyToCollector",
                id,
                (Consumer) (collector) -> privateCall(
                        "addDistributionStatisticSamples",
                        distributionStatisticConfig,
                        collector,
                        timer,
                        (Supplier<Exemplar[]>) timer::exemplars,
                        privateCall("tagValues", id),
                        false
                )
        );
        return timer;
    }

    private Object privateCall(String methodName, Object... args) {
        // Private methds we're using here are unique and haven't overriden versions
        Method method = Arrays.stream(PrometheusMeterRegistry.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst().get();

        try {
            method.setAccessible(true);
            return method.invoke(this, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            method.setAccessible(false);
        }
    }
}
