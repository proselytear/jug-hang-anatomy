package net.proselyte.demo.hanganatomy.resilience;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

    @Bean
    BulkheadRegistry bulkheadRegistry() {
        return BulkheadRegistry.ofDefaults();
    }

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    Bulkhead partnerBulkhead(BulkheadRegistry bulkheadRegistry, HangDemoProperties properties) {
        var cfg = properties.partner().bulkhead();
        return bulkheadRegistry.bulkhead("partnerApi", BulkheadConfig.custom()
                .maxConcurrentCalls(cfg.maxConcurrentCalls())
                .maxWaitDuration(cfg.maxWaitDuration())
                .writableStackTraceEnabled(false)
                .build());
    }

    @Bean
    Bulkhead dashboardDbBulkhead(BulkheadRegistry bulkheadRegistry, HangDemoProperties properties) {
        var cfg = properties.db().bulkhead();
        return bulkheadRegistry.bulkhead("dashboardDb", BulkheadConfig.custom()
                .maxConcurrentCalls(cfg.maxConcurrentCalls())
                .maxWaitDuration(cfg.maxWaitDuration())
                .writableStackTraceEnabled(false)
                .build());
    }

    @Bean
    CircuitBreaker partnerCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry, HangDemoProperties properties) {
        var cfg = properties.partner().circuitBreaker();
        return circuitBreakerRegistry.circuitBreaker("partnerApi", CircuitBreakerConfig.custom()
                .slidingWindowSize(cfg.slidingWindowSize())
                .minimumNumberOfCalls(cfg.minimumNumberOfCalls())
                .failureRateThreshold(cfg.failureRateThreshold())
                .slowCallDurationThreshold(cfg.slowCallDurationThreshold())
                .slowCallRateThreshold(cfg.slowCallRateThreshold())
                .waitDurationInOpenState(cfg.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(cfg.permittedNumberOfCallsInHalfOpenState())
                .build());
    }

    @Bean
    RateLimiter heavyEndpointRateLimiter(RateLimiterRegistry rateLimiterRegistry, HangDemoProperties properties) {
        var cfg = properties.rateLimiter().heavy();
        return rateLimiterRegistry.rateLimiter("heavyEndpoint", RateLimiterConfig.custom()
                .limitForPeriod(cfg.limitForPeriod())
                .limitRefreshPeriod(cfg.limitRefreshPeriod())
                .timeoutDuration(cfg.timeoutDuration())
                .build());
    }

    @Bean
    MeterBinder resilienceMeters(
            BulkheadRegistry bulkheadRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry
    ) {
        return meterRegistry -> {
            TaggedBulkheadMetrics.ofBulkheadRegistry(bulkheadRegistry).bindTo(meterRegistry);
            TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry).bindTo(meterRegistry);
            TaggedRateLimiterMetrics.ofRateLimiterRegistry(rateLimiterRegistry).bindTo(meterRegistry);
        };
    }
}
