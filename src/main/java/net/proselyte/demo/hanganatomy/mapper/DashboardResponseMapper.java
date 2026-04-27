package net.proselyte.demo.hanganatomy.mapper;

import java.util.List;

import net.proselyte.demo.hanganatomy.client.PartnerCallResult;
import net.proselyte.demo.hanganatomy.dto.DashboardItemResponse;
import net.proselyte.demo.hanganatomy.dto.DashboardResponse;
import net.proselyte.demo.hanganatomy.dto.PartnerPayloadResponse;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.service.DashboardData;
import org.springframework.stereotype.Component;

@Component
public class DashboardResponseMapper {

    public DashboardResponse toResponse(DashboardData data, PartnerCallResult partnerCallResult, DemoMode mode) {
        List<DashboardItemResponse> items = data.items().stream()
                .map(item -> new DashboardItemResponse(item.metricName(), item.metricValue()))
                .toList();

        return new DashboardResponse(
                data.dashboard().id(),
                data.dashboard().customerId(),
                data.dashboard().title(),
                data.dashboard().status(),
                data.dashboard().createdAt(),
                mode.token(),
                partnerCallResult.outcome(),
                items,
                new PartnerPayloadResponse(
                        partnerCallResult.payload().source(),
                        partnerCallResult.payload().externalRef(),
                        partnerCallResult.payload().partnerMode(),
                        partnerCallResult.payload().score(),
                        partnerCallResult.payload().recommendation(),
                        partnerCallResult.payload().payload()
                )
        );
    }
}
