package net.proselyte.demo.hanganatomy.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import net.proselyte.demo.hanganatomy.client.PartnerCallResult;
import net.proselyte.demo.hanganatomy.client.PartnerClient;
import net.proselyte.demo.hanganatomy.client.PartnerPayload;
import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.dto.DashboardResponse;
import net.proselyte.demo.hanganatomy.entity.DashboardRecord;
import net.proselyte.demo.hanganatomy.mapper.DashboardResponseMapper;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.DemoModeRegistry;
import net.proselyte.demo.hanganatomy.service.DashboardData;
import net.proselyte.demo.hanganatomy.service.DashboardReadService;
import net.proselyte.demo.hanganatomy.service.DashboardService;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    @Test
    void returnsMappedResponseWhenDependenciesSucceed() {
        DashboardReadService readService = mock(DashboardReadService.class);
        PartnerClient partnerClient = mock(PartnerClient.class);
        DashboardResponseMapper mapper = mock(DashboardResponseMapper.class);
        DemoModeRegistry demoModeRegistry = mock(DemoModeRegistry.class);
        HangDemoProperties properties = TestPropertiesFactory.defaults();

        UUID dashboardId = UUID.randomUUID();
        DashboardData data = new DashboardData(new DashboardRecord(dashboardId, UUID.randomUUID(), "Demo", "ACTIVE", Instant.now()), List.of());
        PartnerCallResult partner = PartnerCallResult.live(new PartnerPayload("live", "ref-1", "normal", 42.0d, "ok", null));
        DashboardResponse expected = new DashboardResponse(
                dashboardId,
                data.dashboard().customerId(),
                "Demo",
                "ACTIVE",
                data.dashboard().createdAt(),
                "normal",
                "success",
                List.of(),
                new net.proselyte.demo.hanganatomy.dto.PartnerPayloadResponse("live", "ref-1", "normal", 42.0d, "ok", null)
        );

        when(demoModeRegistry.currentMode()).thenReturn(DemoMode.NORMAL);
        when(readService.loadDashboardData(dashboardId, DemoMode.NORMAL)).thenReturn(data);
        when(partnerClient.loadPartnerData(eq(dashboardId), eq(DemoMode.NORMAL), any())).thenReturn(partner);
        when(mapper.toResponse(data, partner, DemoMode.NORMAL)).thenReturn(expected);

        DashboardService service = new DashboardService(readService, partnerClient, mapper, demoModeRegistry, properties);

        DashboardResponse actual = service.getDashboard(dashboardId);

        assertThat(actual).isSameAs(expected);
        verify(readService).loadDashboardData(dashboardId, DemoMode.NORMAL);
        verify(partnerClient).loadPartnerData(eq(dashboardId), eq(DemoMode.NORMAL), any());
    }
}
