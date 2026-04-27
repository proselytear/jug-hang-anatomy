package net.proselyte.demo.hanganatomy.observability;

import java.util.Arrays;

public enum DemoMode {
    NORMAL("normal"),
    SLOW_PARTNER("slow-partner"),
    VERY_SLOW_PARTNER("very-slow-partner"),
    UNSTABLE_PARTNER("unstable-partner"),
    DB_PRESSURE("db-pressure"),
    TX_HOLD("tx-hold"),
    RECOVERY("recovery");

    private final String token;

    DemoMode(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }

    public static DemoMode fromToken(String token) {
        return Arrays.stream(values())
                .filter(mode -> mode.token.equalsIgnoreCase(token))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported demo mode: " + token));
    }
}
