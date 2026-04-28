package net.proselyte.demo.hanganatomy.service;

import java.util.UUID;

import net.proselyte.demo.hanganatomy.client.PartnerCallResult;
import net.proselyte.demo.hanganatomy.client.PartnerClient;
import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.mapper.DashboardResponseMapper;
import net.proselyte.demo.hanganatomy.dto.DashboardResponse;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final DashboardReadService dashboardReadService;
    private final PartnerClient partnerClient;
    private final DashboardResponseMapper dashboardResponseMapper;
    private final DemoModeRegistry demoModeRegistry;
    private final HangDemoProperties properties;

    public DashboardService(
            DashboardReadService dashboardReadService,
            PartnerClient partnerClient,
            DashboardResponseMapper dashboardResponseMapper,
            DemoModeRegistry demoModeRegistry,
            HangDemoProperties properties
    ) {
        this.dashboardReadService = dashboardReadService;
        this.partnerClient = partnerClient;
        this.dashboardResponseMapper = dashboardResponseMapper;
        this.demoModeRegistry = demoModeRegistry;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID id) {
        DemoMode mode = demoModeRegistry.currentMode();
        DashboardData dashboardData = dashboardReadService.loadDashboardData(id, mode);
        PartnerCallResult partnerCallResult = partnerClient.loadPartnerData(
                id,
                mode,
                RequestDeadline.after(properties.partner().overallDeadline())
        );
        return dashboardResponseMapper.toResponse(dashboardData, partnerCallResult, mode);
    }
}
