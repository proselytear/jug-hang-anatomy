package net.proselyte.demo.hanganatomy.client;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import net.proselyte.demo.hanganatomy.entity.PartnerSnapshotRecord;
import net.proselyte.demo.hanganatomy.observability.DemoMode;
import net.proselyte.demo.hanganatomy.observability.PartnerMetrics;
import net.proselyte.demo.hanganatomy.repository.PartnerSnapshotRepository;
import net.proselyte.demo.hanganatomy.service.DependencyTimeoutException;
import net.proselyte.demo.hanganatomy.service.RequestDeadline;
import net.proselyte.demo.hanganatomy.service.ServiceUnavailableException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PartnerClient {

    private static final Logger log = LoggerFactory.getLogger(PartnerClient.class);

    private final RestClient partnerRestClient;
    private final HangDemoProperties properties;
    private final Bulkhead partnerBulkhead;
    private final CircuitBreaker partnerCircuitBreaker;
    private final ScheduledExecutorService timeoutScheduler;
    private final ExecutorService partnerExecutor;
    private final PartnerMetrics partnerMetrics;
    private final PartnerSnapshotRepository partnerSnapshotRepository;
    private final ObjectMapper objectMapper;

    public PartnerClient(
            RestClient partnerRestClient,
            HangDemoProperties properties,
            Bulkhead partnerBulkhead,
            CircuitBreaker partnerCircuitBreaker,
            ScheduledExecutorService timeoutScheduler,
            ExecutorService partnerExecutor,
            PartnerMetrics partnerMetrics,
            PartnerSnapshotRepository partnerSnapshotRepository,
            ObjectMapper objectMapper
    ) {
        this.partnerRestClient = partnerRestClient;
        this.properties = properties;
        this.partnerBulkhead = partnerBulkhead;
        this.partnerCircuitBreaker = partnerCircuitBreaker;
        this.timeoutScheduler = timeoutScheduler;
        this.partnerExecutor = partnerExecutor;
        this.partnerMetrics = partnerMetrics;
        this.partnerSnapshotRepository = partnerSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    public PartnerCallResult loadPartnerData(UUID dashboardId, DemoMode mode, RequestDeadline deadline) {
        Instant startedAt = Instant.now();
        Duration budget = deadline.budget(properties.partner().timeout().duration());
        if (budget.isZero() || budget.isNegative()) {
            partnerMetrics.recordTimeout(mode, Duration.ZERO);
            return fallbackOrThrow(dashboardId, mode, "deadline_exhausted", new DependencyTimeoutException("Overall request deadline exhausted"));
        }

        try {
            PartnerPayload payload = executeWithResilience(dashboardId, mode, budget);
            partnerMetrics.recordSuccess(mode, Duration.between(startedAt, Instant.now()));
            persistSnapshotBestEffort(dashboardId, payload);
            return PartnerCallResult.live(payload);
        } catch (BulkheadFullException ex) {
            partnerMetrics.recordRejection(mode, "bulkhead_rejected");
            return fallbackOrThrow(dashboardId, mode, "fallback-bulkhead", ex);
        } catch (CallNotPermittedException ex) {
            partnerMetrics.recordRejection(mode, "circuit_open");
            return fallbackOrThrow(dashboardId, mode, "fallback-circuit-open", ex);
        } catch (DependencyTimeoutException ex) {
            partnerMetrics.recordTimeout(mode, Duration.between(startedAt, Instant.now()));
            return fallbackOrThrow(dashboardId, mode, "fallback-timeout", ex);
        } catch (RestClientException ex) {
            partnerMetrics.recordError(mode, "http_error", Duration.between(startedAt, Instant.now()));
            return fallbackOrThrow(dashboardId, mode, "fallback-http-error", ex);
        }
    }

    private PartnerPayload executeWithResilience(UUID dashboardId, DemoMode mode, Duration budget) {
        Callable<PartnerPayload> callable = decorate(() -> invokePartnerApi(dashboardId, mode));
        if (!properties.partner().timeout().enabled()) {
            return callUnchecked(callable);
        }

        TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(budget)
                .cancelRunningFuture(true)
                .build());

        try {
            CompletionStage<PartnerPayload> completionStage = timeLimiter.executeCompletionStage(timeoutScheduler, () -> CompletableFuture.supplyAsync(
                    () -> callUnchecked(callable),
                    partnerExecutor
            ));
            return completionStage.toCompletableFuture().get();
        } catch (ExecutionException ex) {
            Throwable cause = unwrap(ex);
            if (cause instanceof java.util.concurrent.TimeoutException || cause instanceof java.util.concurrent.CancellationException) {
                throw new DependencyTimeoutException("Partner call timed out after " + budget, cause);
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ServiceUnavailableException("Partner call failed", cause);
        } catch (Exception ex) {
            Throwable cause = unwrap(ex);
            if (cause instanceof java.util.concurrent.TimeoutException) {
                throw new DependencyTimeoutException("Partner call timed out after " + budget, cause);
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ServiceUnavailableException("Partner call failed", cause);
        }
    }

    private Callable<PartnerPayload> decorate(Callable<PartnerPayload> function) {
        Callable<PartnerPayload> guarded = function;
        if (properties.partner().bulkhead().enabled()) {
            guarded = Bulkhead.decorateCallable(partnerBulkhead, guarded);
        }
        if (properties.partner().circuitBreaker().enabled()) {
            guarded = CircuitBreaker.decorateCallable(partnerCircuitBreaker, guarded);
        }
        return guarded;
    }

    private PartnerPayload invokePartnerApi(UUID dashboardId, DemoMode mode) {
        partnerMetrics.incrementInflight();
        try {
            RestClient.RequestHeadersSpec<?> request = partnerRestClient.get()
                    .uri("/dashboard/{id}", dashboardId)
                    .header("X-Demo-Mode", mode.token());
            if (mode == DemoMode.UNSTABLE_PARTNER) {
                request = request.header("X-Unstable-Bucket", Integer.toString(ThreadLocalRandom.current().nextInt(1, 11)));
            }
            PartnerApiResponse response = request.retrieve().body(PartnerApiResponse.class);
            if (response == null) {
                throw new ServiceUnavailableException("Partner response body is empty");
            }
            return new PartnerPayload(
                    "live",
                    response.externalRef(),
                    response.partnerMode(),
                    response.score(),
                    response.recommendation(),
                    response.payload()
            );
        } finally {
            partnerMetrics.decrementInflight();
        }
    }

    private void persistSnapshotBestEffort(UUID dashboardId, PartnerPayload payload) {
        try {
            partnerSnapshotRepository.saveSnapshot(dashboardId, payload);
        } catch (RuntimeException ex) {
            log.warn("Unable to persist partner snapshot for dashboard {}", dashboardId, ex);
        }
    }

    private PartnerCallResult fallbackOrThrow(UUID dashboardId, DemoMode mode, String outcome, RuntimeException cause) {
        Optional<PartnerSnapshotRecord> snapshot = partnerSnapshotRepository.findLatestByDashboardId(dashboardId);
        if (snapshot.isPresent()) {
            partnerMetrics.recordFallback(mode, outcome);
            return PartnerCallResult.fallback(snapshotToPayload(snapshot.get()), outcome);
        }
        if (cause instanceof DependencyTimeoutException timeoutException) {
            throw timeoutException;
        }
        throw new ServiceUnavailableException("Partner dependency is unavailable", cause);
    }

    private PartnerPayload snapshotToPayload(PartnerSnapshotRecord snapshot) {
        try {
            JsonNode payload = objectMapper.readTree(snapshot.payload());
            String partnerMode = payload.path("partnerMode").asText("snapshot");
            double score = payload.path("score").asDouble(0.0d);
            String recommendation = payload.path("recommendation").asText("cached-snapshot");
            return new PartnerPayload(
                    "snapshot",
                    snapshot.externalRef(),
                    partnerMode,
                    score,
                    recommendation,
                    payload
            );
        } catch (JacksonException ex) {
            throw new IllegalStateException("Unable to parse partner snapshot", ex);
        }
    }

    private PartnerPayload callUnchecked(Callable<PartnerPayload> callable) {
        try {
            return callable.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CompletionException(ex);
        }
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException || current instanceof ExecutionException) {
            if (current.getCause() == null) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }
}
