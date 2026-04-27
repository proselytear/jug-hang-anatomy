package net.proselyte.demo.hanganatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        UUID id,
        UUID customerId,
        String title,
        String status,
        Instant createdAt,
        String mode,
        String outcome,
        List<DashboardItemResponse> items,
        PartnerPayloadResponse partner
) {
}
