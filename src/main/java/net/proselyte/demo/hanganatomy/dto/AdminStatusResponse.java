package net.proselyte.demo.hanganatomy.dto;

import java.time.Instant;

public record AdminStatusResponse(
        String status,
        Instant updatedAt,
        boolean cached,
        String mode
) {
}
