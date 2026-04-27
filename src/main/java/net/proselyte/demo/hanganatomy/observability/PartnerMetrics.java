package net.proselyte.demo.hanganatomy.observability;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PartnerMetrics {

    private final MeterRegistry meterRegistry;
    private final String dependencyTag;
    private final AtomicInteger inflight = new AtomicInteger();

    public PartnerMetrics(MeterRegistry meterRegistry, HangDemoProperties properties) {
        this.meterRegistry = meterRegistry;
        this.dependencyTag = properties.partner().dependencyTag();
        Gauge.builder(MetricsContract.PARTNER_INFLIGHT, inflight, AtomicInteger::get)
                .description("In-flight partner HTTP calls")
                .tag(MetricsContract.TAG_DEPENDENCY, dependencyTag)
                .register(meterRegistry);
    }

    public void incrementInflight() {
        inflight.incrementAndGet();
    }

    public void decrementInflight() {
        inflight.decrementAndGet();
    }

    public void recordSuccess(DemoMode mode, Duration duration) {
        recordDuration(mode, "success", duration);
    }

    public void recordError(DemoMode mode, String outcome, Duration duration) {
        recordDuration(mode, outcome, duration);
        counter(MetricsContract.PARTNER_ERRORS, mode, outcome).increment();
    }

    public void recordTimeout(DemoMode mode, Duration duration) {
        recordDuration(mode, "timeout", duration);
        counter(MetricsContract.PARTNER_TIMEOUTS, mode, "timeout").increment();
    }

    public void recordFallback(DemoMode mode, String outcome) {
        counter(MetricsContract.PARTNER_FALLBACKS, mode, outcome).increment();
    }

    public void recordRejection(DemoMode mode, String outcome) {
        counter(MetricsContract.PARTNER_REJECTIONS, mode, outcome).increment();
    }

    private void recordDuration(DemoMode mode, String outcome, Duration duration) {
        Timer.builder(MetricsContract.PARTNER_DURATION)
                .description("Partner API duration")
                .tags(
                        MetricsContract.TAG_DEPENDENCY, dependencyTag,
                        MetricsContract.TAG_OUTCOME, outcome,
                        MetricsContract.TAG_MODE, mode.token()
                )
                .register(meterRegistry)
                .record(duration);
    }

    private Counter counter(String name, DemoMode mode, String outcome) {
        return Counter.builder(name)
                .tags(
                        MetricsContract.TAG_DEPENDENCY, dependencyTag,
                        MetricsContract.TAG_OUTCOME, outcome,
                        MetricsContract.TAG_MODE, mode.token()
                )
                .register(meterRegistry);
    }
}
