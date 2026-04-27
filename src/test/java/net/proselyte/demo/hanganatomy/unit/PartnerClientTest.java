package net.proselyte.demo.hanganatomy.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.proselyte.demo.hanganatomy.client.PartnerCallResult;
import net.proselyte.demo.hanganatomy.client.PartnerClient;
import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.entity.PartnerSnapshotRecord;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.PartnerMetrics;
import net.proselyte.demo.hanganatomy.repository.PartnerSnapshotRepository;
import net.proselyte.demo.hanganatomy.service.DependencyTimeoutException;
import net.proselyte.demo.hanganatomy.service.RequestDeadline;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class PartnerClientTest {

    private final ExecutorService partnerExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void tearDown() throws InterruptedException {
        partnerExecutor.shutdownNow();
        timeoutScheduler.shutdownNow();
        partnerExecutor.awaitTermination(1, TimeUnit.SECONDS);
        timeoutScheduler.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    void returnsSnapshotFallbackWhenDeadlineAlreadyExhausted() {
        PartnerSnapshotRepository snapshotRepository = mock(PartnerSnapshotRepository.class);
        UUID dashboardId = UUID.randomUUID();
        when(snapshotRepository.findLatestByDashboardId(dashboardId))
                .thenReturn(Optional.of(new PartnerSnapshotRecord(
                        UUID.randomUUID(),
                        dashboardId,
                        "snapshot-ref",
                        "{\"partnerMode\":\"snapshot\",\"score\":11.0,\"recommendation\":\"cached\"}",
                        Instant.now()
                )));

        PartnerClient client = new PartnerClient(
                mock(RestClient.class),
                TestPropertiesFactory.defaults(),
                Bulkhead.of("partner", BulkheadConfig.ofDefaults()),
                CircuitBreaker.of("partner", CircuitBreakerConfig.ofDefaults()),
                timeoutScheduler,
                partnerExecutor,
                new PartnerMetrics(new SimpleMeterRegistry(), TestPropertiesFactory.defaults()),
                snapshotRepository,
                new ObjectMapper()
        );

        PartnerCallResult result = client.loadPartnerData(
                dashboardId,
                DemoMode.NORMAL,
                new RequestDeadline(Instant.now().minusMillis(1))
        );

        assertThat(result.fallback()).isTrue();
        assertThat(result.outcome()).isEqualTo("deadline_exhausted");
    }

    @Test
    void throwsTimeoutWhenDeadlineExhaustedAndNoSnapshotExists() {
        PartnerSnapshotRepository snapshotRepository = mock(PartnerSnapshotRepository.class);
        UUID dashboardId = UUID.randomUUID();
        when(snapshotRepository.findLatestByDashboardId(dashboardId)).thenReturn(Optional.empty());

        PartnerClient client = new PartnerClient(
                mock(RestClient.class),
                TestPropertiesFactory.defaults(),
                Bulkhead.of("partner", BulkheadConfig.ofDefaults()),
                CircuitBreaker.of("partner", CircuitBreakerConfig.ofDefaults()),
                timeoutScheduler,
                partnerExecutor,
                new PartnerMetrics(new SimpleMeterRegistry(), TestPropertiesFactory.defaults()),
                snapshotRepository,
                new ObjectMapper()
        );

        assertThatThrownBy(() -> client.loadPartnerData(
                dashboardId,
                DemoMode.NORMAL,
                new RequestDeadline(Instant.now().minusMillis(1))
        )).isInstanceOf(DependencyTimeoutException.class);
    }

    @Test
    void returnsSnapshotFallbackWhenCircuitBreakerIsOpen() {
        PartnerSnapshotRepository snapshotRepository = mock(PartnerSnapshotRepository.class);
        UUID dashboardId = UUID.randomUUID();
        when(snapshotRepository.findLatestByDashboardId(dashboardId))
                .thenReturn(Optional.of(new PartnerSnapshotRecord(
                        UUID.randomUUID(),
                        dashboardId,
                        "snapshot-ref",
                        "{\"partnerMode\":\"snapshot\",\"score\":11.0,\"recommendation\":\"cached\"}",
                        Instant.now()
                )));

        CircuitBreaker circuitBreaker = CircuitBreaker.of("partner", CircuitBreakerConfig.ofDefaults());
        circuitBreaker.transitionToOpenState();

        RestClient restClient = mock(RestClient.class);
        PartnerClient client = new PartnerClient(
                restClient,
                TestPropertiesFactory.defaults(),
                Bulkhead.of("partner", BulkheadConfig.ofDefaults()),
                circuitBreaker,
                timeoutScheduler,
                partnerExecutor,
                new PartnerMetrics(new SimpleMeterRegistry(), TestPropertiesFactory.defaults()),
                snapshotRepository,
                new ObjectMapper()
        );

        PartnerCallResult result = client.loadPartnerData(
                dashboardId,
                DemoMode.NORMAL,
                RequestDeadline.after(Duration.ofMillis(100))
        );

        assertThat(result.fallback()).isTrue();
        assertThat(result.outcome()).isEqualTo("fallback-circuit-open");
        verify(snapshotRepository).findLatestByDashboardId(eq(dashboardId));
        verify(restClient, never()).get();
    }
}
