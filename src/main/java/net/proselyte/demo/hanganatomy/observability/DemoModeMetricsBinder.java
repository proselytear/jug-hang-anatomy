package net.proselyte.demo.hanganatomy.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
public class DemoModeMetricsBinder implements MeterBinder {

    private final DemoModeRegistry demoModeRegistry;

    public DemoModeMetricsBinder(DemoModeRegistry demoModeRegistry) {
        this.demoModeRegistry = demoModeRegistry;
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        for (DemoMode mode : DemoMode.values()) {
            Gauge.builder(MetricsContract.DEMO_MODE, () -> demoModeRegistry.currentMode() == mode ? 1 : 0)
                    .description("Current demo mode marker gauge")
                    .tag(MetricsContract.TAG_MODE, mode.token())
                    .register(meterRegistry);
        }
    }
}
