package net.proselyte.demo.hanganatomy.entity;

import java.time.Instant;
import java.util.UUID;

public record PartnerSnapshotRecord(
        UUID id,
        UUID dashboardId,
        String externalRef,
        String payload,
        Instant createdAt
) {
}
