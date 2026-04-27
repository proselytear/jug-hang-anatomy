package net.proselyte.demo.hanganatomy.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import net.proselyte.demo.hanganatomy.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(OutputCaptureExtension.class)
class ObservabilityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void heavyEndpointPublishesRedMetricsAndUsesTemplateRoute() {
        localRestClient().get().uri("/api/v1/dashboard/{id}", DASHBOARD_ID).retrieve().toBodilessEntity();

        assertThat(findHttpMeterByTemplate()).isPresent();
        assertThat(meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals("http.server.requests"))
                .flatMap(meter -> meter.getId().getTags().stream())
                .noneMatch(tag -> tag.getValue().contains(DASHBOARD_ID.toString())))
                .isTrue();
    }

    @Test
    void partnerMetricsUseLowCardinalityTagsOnly() {
        localRestClient().get().uri("/api/v1/dashboard/{id}", DASHBOARD_ID).retrieve().toBodilessEntity();

        Meter partnerMeter = meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(MetricsContract.PARTNER_DURATION))
                .findFirst()
                .orElseThrow();

        assertThat(partnerMeter.getId().getTag(MetricsContract.TAG_DEPENDENCY)).isEqualTo("partner-api");
        assertThat(partnerMeter.getId().getTags().stream().map(tag -> tag.getKey()))
                .doesNotContain("dashboardId", "customerId", "requestId");
    }

    @Test
    void logsContainTraceIdAndSpanId(CapturedOutput output) {
        localRestClient().get().uri("/api/v1/dashboard/{id}", DASHBOARD_ID).retrieve().toBodilessEntity();

        assertThat(output.getOut()).contains("\"traceId\":\"");
        assertThat(output.getOut()).contains("\"spanId\":\"");
    }

    @Test
    void customMetricsAreRegistered() {
        assertThat(meterRegistry.find(MetricsContract.PARTNER_INFLIGHT).gauge()).isNotNull();
        assertThat(meterRegistry.find(MetricsContract.DEMO_MODE).gauges()).isNotEmpty();
    }

    private Optional<Meter> findHttpMeterByTemplate() {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals("http.server.requests"))
                .filter(meter -> "/api/v1/dashboard/{id}".equals(meter.getId().getTag("uri"))
                        || "/api/v1/dashboard/{id}".equals(meter.getId().getTag("route")))
                .findFirst();
    }
}
