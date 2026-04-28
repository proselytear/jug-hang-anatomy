package net.proselyte.demo.hanganatomy.health;

import java.time.Duration;
import java.time.Instant;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.repository.AdminStatusRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("dbReadiness")
public class CachedDbReadinessHealthIndicator implements HealthIndicator {

    private final AdminStatusRepository adminStatusRepository;
    private final HangDemoProperties properties;
    private final DemoModeRegistry demoModeRegistry;

    public CachedDbReadinessHealthIndicator(
            AdminStatusRepository adminStatusRepository,
            HangDemoProperties properties,
            DemoModeRegistry demoModeRegistry
    ) {
        this.adminStatusRepository = adminStatusRepository;
        this.properties = properties;
        this.demoModeRegistry = demoModeRegistry;
    }

    @Override
    public Health health() {
        DemoMode mode = demoModeRegistry.currentMode();
        maybeSimulateDelay(mode);
        try {
            Instant dbTime = adminStatusRepository.ping();
            return Health.up()
                    .withDetail("dbTime", dbTime.toString())
                    .withDetail("mode", mode.token())
                    .build();
        } catch (RuntimeException ex) {
            return Health.outOfService()
                    .withDetail("reason", "db_failure")
                    .withDetail("exception", ex.getClass().getSimpleName())
                    .build();
        }
    }

    private void maybeSimulateDelay(DemoMode mode) {
        Duration delay = properties.simulatedReadinessDelay(mode);
        if (delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while simulating readiness delay", ex);
        }
    }
}
