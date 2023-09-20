// Do not change, delegate calls have PROTECTED visibility
package io.micrometer.core.instrument;

import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.SafePrometheusTimer;
import io.prometheus.client.exemplars.Exemplar;
import io.prometheus.client.exemplars.ExemplarSampler;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

// Delegation of protected method calls works only from Java classes from same package as MeterRegistry
@SuppressWarnings("ALL")
public class SafePrometheusMeterRegistry extends MeterRegistry {

    private final MeterRegistry meterRegistry;

    private final PrometheusConfig prometheusConfig;
    private final ExemplarSampler exemplarSampler;

    public SafePrometheusMeterRegistry(PrometheusMeterRegistry meterRegistry) {
        super(meterRegistry.clock);
        this.meterRegistry = meterRegistry;
        this.prometheusConfig = (PrometheusConfig) Objects.requireNonNull(privateField("prometheusConfig"));
        this.exemplarSampler = (ExemplarSampler) privateField("exemplarSampler");
    }

    @Override
    public <T> Gauge newGauge(Meter.Id id, T obj, ToDoubleFunction<T> valueFunction) {
        return meterRegistry.newGauge(id, obj, valueFunction);
    }

    @Override
    public Counter newCounter(Meter.Id id) {
        return meterRegistry.newCounter(id);
    }

    @Override
    @Deprecated
    public LongTaskTimer newLongTaskTimer(Meter.Id id) {
        return meterRegistry.newLongTaskTimer(id);
    }

    @Override
    public LongTaskTimer newLongTaskTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig) {
        return meterRegistry.newLongTaskTimer(id, distributionStatisticConfig);
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

    @Override
    public DistributionSummary newDistributionSummary(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, double scale) {
        return meterRegistry.newDistributionSummary(id, distributionStatisticConfig, scale);
    }

    @Override
    public Meter newMeter(Meter.Id id, Meter.Type type, Iterable<Measurement> measurements) {
        return meterRegistry.newMeter(id, type, measurements);
    }

    @Override
    public <T> TimeGauge newTimeGauge(Meter.Id id, T obj, TimeUnit valueFunctionUnit, ToDoubleFunction<T> valueFunction) {
        return meterRegistry.newTimeGauge(id, obj, valueFunctionUnit, valueFunction);
    }

    @Override
    public <T> FunctionTimer newFunctionTimer(Meter.Id id, T obj, ToLongFunction<T> countFunction, ToDoubleFunction<T> totalTimeFunction, TimeUnit totalTimeFunctionUnit) {
        return meterRegistry.newFunctionTimer(id, obj, countFunction, totalTimeFunction, totalTimeFunctionUnit);
    }

    @Override
    public <T> FunctionCounter newFunctionCounter(Meter.Id id, T obj, ToDoubleFunction<T> countFunction) {
        return meterRegistry.newFunctionCounter(id, obj, countFunction);
    }

    @Override
    public List<Tag> getConventionTags(Meter.Id id) {
        return meterRegistry.getConventionTags(id);
    }

    @Override
    public String getConventionName(Meter.Id id) {
        return meterRegistry.getConventionName(id);
    }

    @Override
    public TimeUnit getBaseTimeUnit() {
        return meterRegistry.getBaseTimeUnit();
    }

    @Override
    public DistributionStatisticConfig defaultHistogramConfig() {
        return meterRegistry.defaultHistogramConfig();
    }

    private Object privateCall(String methodName, Object... args) {
        // Private methds we're using here are unique and haven't overriden versions
        Method method = Arrays.stream(PrometheusMeterRegistry.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst().get();

        try {
            method.setAccessible(true);
            return method.invoke(meterRegistry, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            method.setAccessible(false);
        }
    }

    @Nullable
    private Object privateField(String fieldName) {
        Field field = ReflectionUtils.findField(PrometheusMeterRegistry.class, fieldName);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(meterRegistry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            field.setAccessible(false);
        }
    }
}
