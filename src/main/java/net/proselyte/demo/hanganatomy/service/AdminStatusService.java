package net.proselyte.demo.hanganatomy.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.dto.AdminStatusResponse;
import net.proselyte.demo.hanganatomy.entity.AdminStatusRecord;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.observability.MetricsContract;
import net.proselyte.demo.hanganatomy.repository.AdminStatusRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class AdminStatusService {

    private final AdminStatusRepository adminStatusRepository;
    private final DemoModeRegistry demoModeRegistry;
    private final HangDemoProperties properties;
    private final MeterRegistry meterRegistry;
    private final ExecutorService budgetExecutor;
    private final AtomicReference<CachedStatus> cachedStatus = new AtomicReference<>();

    public AdminStatusService(
            AdminStatusRepository adminStatusRepository,
            DemoModeRegistry demoModeRegistry,
            HangDemoProperties properties,
            MeterRegistry meterRegistry,
            ExecutorService budgetExecutor
    ) {
        this.adminStatusRepository = adminStatusRepository;
        this.demoModeRegistry = demoModeRegistry;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.budgetExecutor = budgetExecutor;
    }

    public AdminStatusResponse getStatus() {
        DemoMode mode = demoModeRegistry.currentMode();
        maybeSimulateDelay(mode);
        AdminStatusRecord current = adminStatusRepository.loadCurrent();
        return toResponse(current, false);
    }

    private AdminStatusRecord loadCurrentWithBudget(DemoMode mode) {
        Future<AdminStatusRecord> future = budgetExecutor.submit(() -> {
            maybeSimulateDelay(mode);
            return adminStatusRepository.loadCurrent();
        });
        try {
            return future.get(properties.admin().dbTimeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new DependencyTimeoutException("Admin status query exceeded budget", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DependencyTimeoutException("Admin status query interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new ServiceUnavailableException("Admin status query failed: " + cause.getMessage(), cause);
        }
    }

    private void maybeSimulateDelay(DemoMode mode) {
        Duration delay = properties.simulatedAdminDelay(mode);
        if (delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DependencyTimeoutException("Interrupted while simulating admin delay", ex);
        }
    }

    private AdminStatusResponse toResponse(AdminStatusRecord record, boolean cached) {
        return new AdminStatusResponse(record.status(), record.updatedAt(), cached, demoModeRegistry.currentMode().token());
    }

    private void recordDuration(String outcome, Duration duration) {
        Timer.builder(MetricsContract.ADMIN_STATUS_DURATION)
                .tags(
                        MetricsContract.TAG_ENDPOINT, "/api/v1/admin/status",
                        MetricsContract.TAG_OUTCOME, outcome,
                        MetricsContract.TAG_MODE, demoModeRegistry.currentMode().token()
                )
                .register(meterRegistry)
                .record(duration);
    }

    private record CachedStatus(AdminStatusRecord record, Instant loadedAt) {
        boolean isFresh(Duration ttl) {
            return loadedAt.plus(ttl).isAfter(Instant.now());
        }
    }
}
