package net.proselyte.demo.hanganatomy.config;

import java.time.Duration;
import java.util.Map;

import net.proselyte.demo.hanganatomy.observability.DemoMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hang")
public record HangDemoProperties(
        Demo demo,
        Partner partner,
        Db db,
        RateLimiterGroup rateLimiter,
        Admin admin,
        Health health
) {

    public Duration simulatedDbDelay(DemoMode mode) {
        return demo.simulatedDbDelay().getOrDefault(mode, Duration.ZERO);
    }

    public record Demo(
            DemoMode defaultMode,
            Map<DemoMode, Duration> simulatedDbDelay
    ) {
    }

    public record Partner(
            String baseUrl,
            String dependencyTag,
            Duration connectTimeout,
            Duration readTimeout,
            Duration overallDeadline,
            Timeout timeout,
            Bulkhead bulkhead,
            CircuitBreaker circuitBreaker
    ) {
    }

    public record Db(
            Bulkhead bulkhead
    ) {
    }

    public record RateLimiterGroup(
            Heavy heavy
    ) {
    }

    public record Admin(
            Duration cacheTtl,
            Duration dbTimeout
    ) {
    }

    public record Health(
            Duration readinessCacheTtl,
            Duration readinessTimeout,
            String dbQuery
    ) {
    }

    public record Timeout(
            boolean enabled,
            Duration duration
    ) {
    }

    public record Bulkhead(
            boolean enabled,
            int maxConcurrentCalls,
            Duration maxWaitDuration
    ) {
    }

    public record CircuitBreaker(
            boolean enabled,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            float failureRateThreshold,
            Duration slowCallDurationThreshold,
            float slowCallRateThreshold,
            Duration waitDurationInOpenState,
            int permittedNumberOfCallsInHalfOpenState
    ) {
    }

    public record Heavy(
            boolean enabled,
            int limitForPeriod,
            Duration limitRefreshPeriod,
            Duration timeoutDuration
    ) {
    }
}
