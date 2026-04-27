package net.proselyte.demo.hanganatomy.service;

import java.util.UUID;

import net.proselyte.demo.hanganatomy.client.PartnerCallResult;
import net.proselyte.demo.hanganatomy.client.PartnerClient;
import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.mapper.DashboardResponseMapper;
import net.proselyte.demo.hanganatomy.dto.DashboardResponse;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.observability.MetricsContract;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DashboardReadService dashboardReadService;
    private final PartnerClient partnerClient;
    private final DashboardResponseMapper dashboardResponseMapper;
    private final DemoModeRegistry demoModeRegistry;
    private final HangDemoProperties properties;
    private final Bulkhead dashboardDbBulkhead;
    private final RateLimiter heavyEndpointRateLimiter;
    private final MeterRegistry meterRegistry;

    public DashboardService(
            DashboardReadService dashboardReadService,
            PartnerClient partnerClient,
            DashboardResponseMapper dashboardResponseMapper,
            DemoModeRegistry demoModeRegistry,
            HangDemoProperties properties,
            @Qualifier("dashboardDbBulkhead") Bulkhead dashboardDbBulkhead,
            @Qualifier("heavyEndpointRateLimiter") RateLimiter heavyEndpointRateLimiter,
            MeterRegistry meterRegistry
    ) {
        this.dashboardReadService = dashboardReadService;
        this.partnerClient = partnerClient;
        this.dashboardResponseMapper = dashboardResponseMapper;
        this.demoModeRegistry = demoModeRegistry;
        this.properties = properties;
        this.dashboardDbBulkhead = dashboardDbBulkhead;
        this.heavyEndpointRateLimiter = heavyEndpointRateLimiter;
        this.meterRegistry = meterRegistry;
    }

    public DashboardResponse getDashboard(UUID id) {
        DemoMode mode = demoModeRegistry.currentMode();
        RequestDeadline deadline = RequestDeadline.after(properties.partner().overallDeadline());
        guardRateLimit(mode);
        DashboardData dashboardData = guardedDashboardRead(id, mode);
        PartnerCallResult partnerCallResult = partnerClient.loadPartnerData(id, mode, deadline);
        return dashboardResponseMapper.toResponse(dashboardData, partnerCallResult, mode);
    }

    private void guardRateLimit(DemoMode mode) {
        if (!properties.rateLimiter().heavy().enabled()) {
            return;
        }
        if (!heavyEndpointRateLimiter.acquirePermission()) {
            counter(MetricsContract.RATE_LIMITER_REJECTIONS, "rejected", mode).increment();
            throw new HeavyRequestRejectedException("Heavy endpoint rate limiter rejected the request");
        }
        counter(MetricsContract.RATE_LIMITER_ALLOWED, "allowed", mode).increment();
    }

    private DashboardData guardedDashboardRead(UUID id, DemoMode mode) {
        try {
            if (!properties.db().bulkhead().enabled()) {
                return dashboardReadService.loadDashboardData(id, mode);
            }
            return dashboardDbBulkhead.executeCallable(() -> dashboardReadService.loadDashboardData(id, mode));
        } catch (BulkheadFullException ex) {
            counter(MetricsContract.DB_BULKHEAD_REJECTIONS, "rejected", mode).increment();
            throw new HeavyRequestRejectedException("DB bulkhead rejected the request", ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceUnavailableException("Failed to load dashboard data", ex);
        }
    }

    private Counter counter(String name, String outcome, DemoMode mode) {
        return Counter.builder(name)
                .tags(
                        MetricsContract.TAG_ENDPOINT, "/api/v1/dashboard/{id}",
                        MetricsContract.TAG_OUTCOME, outcome,
                        MetricsContract.TAG_MODE, mode.token()
                )
                .register(meterRegistry);
    }
}
