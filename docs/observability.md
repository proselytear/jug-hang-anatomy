# Observability

## Metric Model

The project uses a strict low-cardinality contract defined in `MetricsContract`.

Allowed tags:

- `endpoint`
- `outcome`
- `dependency`
- `mode`

Explicitly forbidden in metrics:

- `dashboardId`
- `customerId`
- `requestId`

## RED

HTTP RED comes from Actuator and Micrometer:

- request rate by route template
- error rate by route template
- p50, p95 and p99 latency by route template

Important convention:

- dashboards and alerts use route templates such as `/api/v1/dashboard/{id}`
- they do not use raw UUID paths

## USE

Tomcat:

- `tomcat_threads_busy`
- `tomcat_threads_current`
- `tomcat_threads_config_max`

Hikari:

- `hikaricp_connections_active`
- `hikaricp_connections_idle`
- `hikaricp_connections_pending`
- `hikaricp_connections_max`
- `hikaricp_connections_timeout_total`

Partner:

- `hang_partner_duration_seconds`
- `hang_partner_errors_total`
- `hang_partner_timeouts_total`
- `hang_partner_inflight`
- `hang_partner_rejections_total`
- `hang_partner_fallbacks_total`
- Resilience4j metrics for bulkhead, circuit breaker and rate limiter

Executor pools:

- `executor_active_threads{name="partner-executor"}`
- `executor_queued_tasks{name="partner-executor"}`
- `executor_active_threads{name="budget-executor"}`
- `executor_queued_tasks{name="budget-executor"}`

These gauges observe the actual fixed partner pool and cached budget pool. The cached pool uses direct handoff, so a zero queue does not establish that its thread count is bounded. Check active threads alongside it. Executor monitoring was added after the article's original measurements on `8c14914`; those measurements contain no executor queue samples.

## Logs

The application writes JSON logs and Alloy ships them to Loki.

Important fields:

- `timestamp`
- `level`
- `logger`
- `message`
- `traceId`
- `spanId`
- `endpoint`
- `mode`
- `outcome`
- `durationMs`

For dashboard responses, the controller passes `DashboardResponse.outcome` through a request attribute to the logging interceptor. HTTP 200 with a saved snapshot is therefore logged as `fallback-bulkhead`, `fallback-timeout` or `fallback-circuit-open`, rather than `success`. For routes without an explicit outcome, the interceptor retains its HTTP-status classification. This behavior was added after commit `8c14914`.

## Traces

Tracing is enabled through OTLP export to Alloy and then to Tempo.

Expected spans:

- inbound HTTP request span
- partner HTTP client span
- downstream DB work, depending on what the runtime auto-instruments

## Backends

Signal path:

1. app exposes `/actuator/prometheus`
2. Alloy scrapes Prometheus-compatible metrics and remote-writes to Mimir
3. app exports OTLP traces to Alloy, Alloy forwards them to Tempo
4. app writes JSON logs to a shared volume, Alloy tails them and pushes them to Loki
5. Grafana ships with provisioned datasources for all three backends

## Dashboards

- `Hang Anatomy - Overview`: RED + CPU + memory + mode
- `Hang Anatomy - Three Queues`: Tomcat + Hikari + partner queue view
- `Hang Anatomy - Partner Resilience`: bulkhead, timeouts, circuit breaker, fallback
- `Hang Anatomy - Health and Admin`: control-plane latency and failures
- `Hang Anatomy - Virtual Threads Experiment`: VT before/after comparison
- `Hang Anatomy - Final Reference`: reference operating picture

## Alerts

Provisioned Grafana-managed rules:

- `HighEndpointLatencyP99`
- `AdminEndpointDegraded`
- `ActuatorHealthDegraded`
- `TomcatThreadSaturation`
- `HikariPendingConnections`
- `PartnerLatencyHigh`
- `BulkheadRejecting`
- `CircuitBreakerOpen`
- `RateLimiterRejecting`

The alerts are intended to be visible in Grafana after `make up`; they are not designed to send real notifications in this local demo.
