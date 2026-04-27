package net.proselyte.demo.hanganatomy.unit;

import java.time.Duration;
import java.util.Map;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.observability.DemoMode;

final class TestPropertiesFactory {

    private TestPropertiesFactory() {
    }

    static HangDemoProperties defaults() {
        return new HangDemoProperties(
                new HangDemoProperties.Demo(DemoMode.NORMAL, Map.of(DemoMode.NORMAL, Duration.ZERO)),
                new HangDemoProperties.Partner(
                        "http://localhost:8089/partner-api/v1",
                        "partner-api",
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(200),
                        new HangDemoProperties.Timeout(true, Duration.ofMillis(150)),
                        new HangDemoProperties.Bulkhead(true, 2, Duration.ZERO),
                        new HangDemoProperties.CircuitBreaker(true, 10, 5, 50.0f, Duration.ofMillis(100), 50.0f, Duration.ofSeconds(5), 2)
                ),
                new HangDemoProperties.Db(
                        new HangDemoProperties.Bulkhead(true, 2, Duration.ZERO)
                ),
                new HangDemoProperties.RateLimiterGroup(
                        new HangDemoProperties.Heavy(true, 5, Duration.ofSeconds(1), Duration.ZERO)
                ),
                new HangDemoProperties.Admin(Duration.ZERO, Duration.ofMillis(100), Map.of()),
                new HangDemoProperties.Health(Duration.ofSeconds(1), Duration.ofMillis(100), "select now()", Map.of())
        );
    }
}
