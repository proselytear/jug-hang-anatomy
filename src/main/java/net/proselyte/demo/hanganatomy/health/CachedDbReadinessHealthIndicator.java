package net.proselyte.demo.hanganatomy.health;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.observability.MetricsContract;
import net.proselyte.demo.hanganatomy.repository.AdminStatusRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("dbReadiness")
public class CachedDbReadinessHealthIndicator implements HealthIndicator {

    private final AdminStatusRepository adminStatusRepository;
    private final HangDemoProperties properties;
    private final MeterRegistry meterRegistry;
    private final ExecutorService budgetExecutor;
    private final DemoModeRegistry demoModeRegistry;
    private final AtomicReference<CachedHealth> cachedHealth = new AtomicReference<>();

    public CachedDbReadinessHealthIndicator(
            AdminStatusRepository adminStatusRepository,
            HangDemoProperties properties,
            MeterRegistry meterRegistry,
            ExecutorService budgetExecutor,
            DemoModeRegistry demoModeRegistry
    ) {
        this.adminStatusRepository = adminStatusRepository;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.budgetExecutor = budgetExecutor;
        this.demoModeRegistry = demoModeRegistry;
    }

    @Override
    public Health health() {
        try {
            Instant dbTime = adminStatusRepository.ping();
            return Health.up()
                    .withDetail("dbTime", dbTime.toString())
                    .withDetail("mode", demoModeRegistry.currentMode().token())
                    .build();
        } catch (RuntimeException ex) {
            return Health.outOfService()
                    .withDetail("reason", "db_failure")
                    .withDetail("exception", ex.getClass().getSimpleName())
                    .build();
        }
    }

    private Health down(String outcome, Instant startedAt, Throwable throwable) {
        recordDuration(outcome, Duration.between(startedAt, Instant.now()));
        Counter.builder(MetricsContract.HEALTH_READINESS_FAILURES)
                .tags(
                        MetricsContract.TAG_ENDPOINT, "/actuator/health/readiness",
                        MetricsContract.TAG_OUTCOME, outcome,
                        MetricsContract.TAG_MODE, demoModeRegistry.currentMode().token()
                )
                .register(meterRegistry)
                .increment();
        Health health = Health.outOfService()
                .withDetail("reason", outcome)
                .withDetail("exception", throwable.getClass().getSimpleName())
                .build();
        cachedHealth.set(new CachedHealth(health, Instant.now()));
        return health;
    }

    private void recordDuration(String outcome, Duration duration) {
        Timer.builder(MetricsContract.HEALTH_READINESS_DURATION)
                .tags(
                        MetricsContract.TAG_ENDPOINT, "/actuator/health/readiness",
                        MetricsContract.TAG_OUTCOME, outcome,
                        MetricsContract.TAG_MODE, demoModeRegistry.currentMode().token()
                )
                .register(meterRegistry)
                .record(duration);
    }

    private record CachedHealth(Health health, Instant loadedAt) {
        boolean isFresh(Duration ttl) {
            return loadedAt.plus(ttl).isAfter(Instant.now());
        }
    }
}
