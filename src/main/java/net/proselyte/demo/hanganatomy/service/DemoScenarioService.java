package net.proselyte.demo.hanganatomy.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.dto.DemoConfigResponse;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.repository.DashboardRepository;
import org.springframework.stereotype.Service;

@Service
public class DemoScenarioService {

    private final DemoModeRegistry demoModeRegistry;
    private final DashboardRepository dashboardRepository;
    private final HangDemoProperties properties;

    public DemoScenarioService(
            DemoModeRegistry demoModeRegistry,
            DashboardRepository dashboardRepository,
            HangDemoProperties properties
    ) {
        this.demoModeRegistry = demoModeRegistry;
        this.dashboardRepository = dashboardRepository;
        this.properties = properties;
    }

    public DemoConfigResponse currentConfig() {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("partnerBulkheadEnabled", properties.partner().bulkhead().enabled());
        features.put("partnerCircuitBreakerEnabled", properties.partner().circuitBreaker().enabled());
        features.put("timeoutEnabled", properties.partner().timeout().enabled());
        features.put("dbBulkheadEnabled", properties.db().bulkhead().enabled());
        features.put("heavyRateLimiterEnabled", properties.rateLimiter().heavy().enabled());
        features.put("virtualThreadsProfile", "vt");
        return new DemoConfigResponse(
                demoModeRegistry.currentMode().token(),
                dashboardRepository.sampleIds(5).stream().map(Object::toString).toList(),
                features
        );
    }

    public DemoConfigResponse switchMode(DemoMode mode) {
        demoModeRegistry.switchTo(mode);
        return currentConfig();
    }
}
