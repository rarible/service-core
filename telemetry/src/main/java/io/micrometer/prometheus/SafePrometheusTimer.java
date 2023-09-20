// Do not change, some classes have PACKAGE visibility
package io.micrometer.prometheus;

import io.micrometer.core.instrument.AbstractTimer;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.FixedBoundaryVictoriaMetricsHistogram;
import io.micrometer.core.instrument.distribution.Histogram;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.TimeWindowMax;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.instrument.util.TimeUtils;
import io.micrometer.core.lang.Nullable;
import io.prometheus.client.exemplars.Exemplar;
import io.prometheus.client.exemplars.HistogramExemplarSampler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Copy of PrometheusTimer with fix of totalTime overflow
 */
public class SafePrometheusTimer extends AbstractTimer {

    private static final long NS_IN_S = 1000 * 1000 * 1000;
    private static final CountAtBucket[] EMPTY_HISTOGRAM = new CountAtBucket[0];

    private final LongAdder count = new LongAdder();

    // CHANGE: totalTime split into s/ns
    private final LongAdder totalTimeNanos = new LongAdder();
    private final LongAdder totalTimeSeconds = new LongAdder();

    private final TimeWindowMax max;

    private final HistogramFlavor histogramFlavor;

    @Nullable
    private final Histogram histogram;

    private boolean exemplarsEnabled = false;

    public SafePrometheusTimer(
            Id id,
            Clock clock,
            DistributionStatisticConfig distributionStatisticConfig,
            PauseDetector pauseDetector,
            HistogramFlavor histogramFlavor,
            @Nullable HistogramExemplarSampler exemplarSampler
    ) {
        super(
                id,
                clock,
                DistributionStatisticConfig.builder().percentilesHistogram(false).serviceLevelObjectives()
                        .build().merge(distributionStatisticConfig),
                pauseDetector,
                TimeUnit.SECONDS,
                false
        );

        this.histogramFlavor = histogramFlavor;
        this.max = new TimeWindowMax(clock, distributionStatisticConfig);

        if (distributionStatisticConfig.isPublishingHistogram()) {
            switch (histogramFlavor) {
                case Prometheus:
                    PrometheusHistogram prometheusHistogram = new PrometheusHistogram(clock,
                            distributionStatisticConfig, exemplarSampler);
                    this.histogram = prometheusHistogram;
                    this.exemplarsEnabled = prometheusHistogram.isExemplarsEnabled();
                    break;
                case VictoriaMetrics:
                    this.histogram = new FixedBoundaryVictoriaMetricsHistogram();
                    break;
                default:
                    this.histogram = null;
                    break;
            }
        } else {
            this.histogram = null;
        }
    }

    @Override
    protected void recordNonNegative(long amount, TimeUnit unit) {
        count.increment();
        long nanoAmount = TimeUnit.NANOSECONDS.convert(amount, unit);

        // CHANGED: separate counters for nanos/seconds, but their sum is the same
        totalTimeSeconds.add(nanoAmount / NS_IN_S);
        totalTimeNanos.add(nanoAmount % NS_IN_S);

        // NOTE: Kept 'as is', overflow is not possible here
        max.record(nanoAmount, TimeUnit.NANOSECONDS);

        // NOTE: Kept 'as is', overflow is not possible here
        if (histogram != null) {
            histogram.recordLong(TimeUnit.NANOSECONDS.convert(amount, unit));
        }
    }

    @Nullable
    public Exemplar[] exemplars() {
        if (exemplarsEnabled) {
            return ((PrometheusHistogram) histogram).exemplars();
        } else {
            return null;
        }
    }

    @Override
    public long count() {
        return count.longValue();
    }

    @Override
    public double totalTime(TimeUnit unit) {
        // CHANGED: now there is a sum of two adders, we can lose some ns during conversion to double,
        // but for large values it doesn't really matter. For small values precision is the same, without losses
        double totalNanos = totalTimeSeconds.doubleValue() * NS_IN_S + totalTimeNanos.doubleValue();
        return TimeUtils.nanosToUnit(totalNanos, unit);
    }

    @Override
    public double max(TimeUnit unit) {
        return max.poll(unit);
    }

    public HistogramFlavor histogramFlavor() {
        return histogramFlavor;
    }

    public CountAtBucket[] histogramCounts() {
        return histogram == null ? EMPTY_HISTOGRAM : histogram.takeSnapshot(0, 0, 0).histogramCounts();
    }

    @Override
    public HistogramSnapshot takeSnapshot() {
        HistogramSnapshot snapshot = super.takeSnapshot();

        if (histogram == null) {
            return snapshot;
        }

        return new HistogramSnapshot(
                snapshot.count(),
                snapshot.total(),
                snapshot.max(), snapshot.percentileValues(),
                histogramCounts(), snapshot::outputSummary
        );
    }

}
