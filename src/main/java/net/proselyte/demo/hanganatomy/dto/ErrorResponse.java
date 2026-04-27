package net.proselyte.demo.hanganatomy.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String traceId,
        String mode
) {
}
