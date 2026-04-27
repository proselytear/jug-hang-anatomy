# Architecture

## C4 Context

The system is intentionally small:

- end users call a blocking Spring MVC application
- the application reads PostgreSQL through HikariCP
- the application calls a partner HTTP API simulated by WireMock
- logs, metrics and traces flow into Loki, Mimir and Tempo through Grafana Alloy
- Grafana visualizes incidents and alert rules

See [docs/diagrams/c4-context.puml](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/diagrams/c4-context.puml).

## C4 Container

Containers in the local demo:

- `app`: Spring Boot 4 MVC service
- `postgres`: primary relational database
- `wiremock`: partner API simulator
- `alloy`: log / metric / trace collector
- `loki`: log backend
- `mimir`: metric backend
- `tempo`: trace backend
- `grafana`: dashboards and alerts
- `jmeter`: optional load generator

See [docs/diagrams/c4-container.puml](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/diagrams/c4-container.puml).

## Components

Inside the application:

- `DashboardController`: heavy endpoint entry point
- `DashboardService`: orchestration, rate limiter and DB bulkhead
- `DashboardReadService`: transactional DB read
- `PartnerClient`: HTTP dependency, partner bulkhead, timeout and circuit breaker
- `AdminStatusService`: cached budgeted admin status path
- `CachedDbReadinessHealthIndicator`: readiness with timeout and cache
- repositories: thin JDBC access layer
- `RequestLoggingInterceptor` and `PartnerMetrics`: low-cardinality observability contract

## Key Sequences

Baseline bad request:

- DB work and partner call live in one long transaction
- one Tomcat worker holds a Hikari connection while waiting on HTTP
- under slow partner, Hikari active and pending grow
- admin and readiness compete for the same exhausted pool

See [docs/diagrams/sequence-baseline-heavy.puml](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/diagrams/sequence-baseline-heavy.puml).

Bulkhead-protected partner call:

- heavy request reads DB first
- partner call goes through a semaphore bulkhead
- overflow is rejected early and can use cached snapshot fallback

See [docs/diagrams/sequence-bulkhead.puml](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/diagrams/sequence-bulkhead.puml).

Final reference flow:

- inbound rate limiter caps load
- DB read is short and isolated
- partner call has bulkhead, timeout, deadline and circuit breaker
- admin and readiness use their own tight budgets and cached fallbacks

See [docs/diagrams/sequence-final-heavy.puml](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/diagrams/sequence-final-heavy.puml).
