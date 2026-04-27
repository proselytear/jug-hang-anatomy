package net.proselyte.demo.hanganatomy.entity;

import java.time.Instant;
import java.util.UUID;

public record DashboardRecord(
        UUID id,
        UUID customerId,
        String title,
        String status,
        Instant createdAt
) {
}
