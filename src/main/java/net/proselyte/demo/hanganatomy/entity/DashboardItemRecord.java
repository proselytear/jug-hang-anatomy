package net.proselyte.demo.hanganatomy.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DashboardItemRecord(
        UUID id,
        UUID dashboardId,
        String metricName,
        BigDecimal metricValue,
        Instant createdAt
) {
}
