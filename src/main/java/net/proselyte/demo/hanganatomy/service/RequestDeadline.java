package net.proselyte.demo.hanganatomy.service;

import java.time.Duration;
import java.time.Instant;

public record RequestDeadline(
        Instant deadlineAt
) {

    public static RequestDeadline after(Duration duration) {
        return new RequestDeadline(Instant.now().plus(duration));
    }

    public Duration remaining() {
        Duration remaining = Duration.between(Instant.now(), deadlineAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public Duration budget(Duration preferred) {
        Duration remaining = remaining();
        if (remaining.isZero()) {
            return Duration.ZERO;
        }
        return remaining.compareTo(preferred) < 0 ? remaining : preferred;
    }
}
