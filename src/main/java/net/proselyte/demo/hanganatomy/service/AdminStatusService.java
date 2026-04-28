package net.proselyte.demo.hanganatomy.service;

import java.time.Duration;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.dto.AdminStatusResponse;
import net.proselyte.demo.hanganatomy.entity.AdminStatusRecord;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.repository.AdminStatusRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminStatusService {

    private final AdminStatusRepository adminStatusRepository;
    private final DemoModeRegistry demoModeRegistry;
    private final HangDemoProperties properties;

    public AdminStatusService(
            AdminStatusRepository adminStatusRepository,
            DemoModeRegistry demoModeRegistry,
            HangDemoProperties properties
    ) {
        this.adminStatusRepository = adminStatusRepository;
        this.demoModeRegistry = demoModeRegistry;
        this.properties = properties;
    }

    public AdminStatusResponse getStatus() {
        DemoMode mode = demoModeRegistry.currentMode();
        maybeSimulateDelay(mode);
        AdminStatusRecord current = adminStatusRepository.loadCurrent();
        return toResponse(current, false);
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
}
