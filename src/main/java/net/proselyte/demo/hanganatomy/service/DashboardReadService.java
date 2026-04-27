package net.proselyte.demo.hanganatomy.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.entity.DashboardItemRecord;
import net.proselyte.demo.hanganatomy.entity.DashboardRecord;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.repository.DashboardItemRepository;
import net.proselyte.demo.hanganatomy.repository.DashboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardReadService {

    private final DashboardRepository dashboardRepository;
    private final DashboardItemRepository dashboardItemRepository;
    private final HangDemoProperties properties;

    public DashboardReadService(
            DashboardRepository dashboardRepository,
            DashboardItemRepository dashboardItemRepository,
            HangDemoProperties properties
    ) {
        this.dashboardRepository = dashboardRepository;
        this.dashboardItemRepository = dashboardItemRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public DashboardData loadDashboardData(UUID id, DemoMode mode) {
        DashboardRecord dashboard = dashboardRepository.findByIdRequired(id);
        List<DashboardItemRecord> items = dashboardItemRepository.findByDashboardId(id);
        maybeSimulateDbDelay(mode);
        return new DashboardData(dashboard, items);
    }

    private void maybeSimulateDbDelay(DemoMode mode) {
        Duration delay = properties.simulatedDbDelay(mode);
        if (delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DependencyTimeoutException("Interrupted while simulating DB pressure", ex);
        }
    }
}
