package net.proselyte.demo.hanganatomy.entity;

import java.time.Instant;

public record AdminStatusRecord(
        long id,
        String status,
        Instant updatedAt
) {
}
