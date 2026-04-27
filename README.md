# hang-anatomy-demo

Production-like demo repository for the conference talk:

`Анатомия зависания: когда thread pool закончился, а CPU почти пустой`

The project demonstrates a blocking Spring MVC service on embedded Tomcat, three waiting queues, and the treatment path from baseline incident to a controlled final reference:

- Tomcat worker thread saturation
- HikariCP connection starvation
- Slow or unstable external partner latency / in-flight growth / timeouts

The main branch contains the final reference state. The `demo/*` branches are intended to mirror the talk progression from the intentionally bad baseline to the hardened implementation.

## Stack

- Java 25
- Spring Boot 4.0.6
- Spring MVC + embedded Tomcat
- Gradle 9.1.0 wrapper
- PostgreSQL 17
- Flyway
- HikariCP
- Micrometer + Actuator
- OpenTelemetry OTLP traces
- Resilience4j
- WireMock
- Grafana + Loki + Mimir + Tempo + Alloy
- JMeter 5.5 container runner
- Docker Compose
- Makefile

## Compatibility Notes

- Spring Boot `4.0.6` is used as the current stable Spring Boot 4 release.
- Spring Boot 4 uses Jackson 3 packages (`tools.jackson.*`), so the project is already aligned to that namespace.
- Gradle wrapper is pinned to `9.1.0`, because older Gradle lines were not stable enough in this environment with Java 25.
- The Docker runtime image uses `eclipse-temurin:25-jdk` instead of a smaller JRE image. This keeps Java 25 availability predictable; see [docs/troubleshooting.md](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/troubleshooting.md) for rationale.
- The load runner is pinned to `justb4/jmeter:5.5` because newer `5.6.x` tags were not reliably available from public registries in this environment.

## Quick Start

Prerequisites:

- Docker Desktop or a compatible Docker engine with Compose
- `curl`
- `rg`
- `make`

Run the full demo:

```bash
make up
```

Then open:

- App: [http://localhost:8080](http://localhost:8080)
- Grafana: [http://localhost:3000](http://localhost:3000)
- WireMock: [http://localhost:8089](http://localhost:8089)

Grafana credentials:

- username: `admin`
- password: `admin`

## Main Endpoints

- `GET /api/v1/dashboard/{id}`: heavy endpoint
- `GET /api/v1/admin/status`: lightweight operational endpoint
- `POST /api/v1/demo/mode/{mode}`: switch the in-app demo mode
- `GET /api/v1/demo/config`: inspect current mode
- `GET /actuator/health`
- `GET /actuator/health/readiness`
- `GET /actuator/health/liveness`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`
- `GET /actuator/info`

Example dashboard id:

- `00000000-0000-0000-0000-000000000001`

## Demo Modes

- `normal`
- `slow-partner`
- `very-slow-partner`
- `unstable-partner`
- `db-pressure`
- `tx-hold`
- `recovery`

These modes affect both WireMock behavior and the app-level mode tag used in logs and metrics.
The Docker demo stack enables the `demo-latency` Spring profile by default so incident modes produce clearly visible 5-15 second delays instead of sub-second noise.

## Make Targets

Primary workflow:

- `make help`
- `make build`
- `make test`
- `make up`
- `make smoke`
- `make down`
- `make clean-volumes`
- `make restart`
- `make logs`
- `make logs-infra`
- `make ps`
- `make grafana`
- `make dashboard-urls`

Partner switching:

- `make partner-normal`
- `make partner-slow`
- `make partner-very-slow`
- `make partner-unstable`
- `make partner-recovery`
- `make mode-db-pressure`
- `make mode-tx-hold`

Load tests:

- `make test-baseline-load`
- `make test-mixed-load`
- `make test-slow-partner-load`
- `make test-db-pressure-load`
- `make test-final-load`

Talk branches:

- `make demo-baseline`
- `make demo-bulkhead`
- `make demo-vt`
- `make demo-final`

## Architecture Overview

Request path for the heavy endpoint:

1. Tomcat accepts a blocking MVC request.
2. `DashboardService` rate-limits and DB-bulkheads the flow.
3. `DashboardReadService` loads dashboard rows from PostgreSQL through HikariCP.
4. `PartnerClient` calls WireMock through HTTP and applies partner bulkhead, timeout and circuit breaker.
5. The response is assembled into a single DTO and returned to the caller.
6. Micrometer, OpenTelemetry and JSON logs export the same incident through metrics, traces and logs.

More detail:

- [docs/architecture.md](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/architecture.md)
- [docs/observability.md](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/observability.md)
- [docs/talk-scenario.md](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/talk-scenario.md)

## Demo Scenario

Fast path:

```bash
make up
make partner-slow
make test-mixed-load
make dashboard-urls
```

What to show:

1. `Hang Anatomy - Overview`: latency grows while CPU is not the main signal.
2. `Hang Anatomy - Three Queues`: Tomcat busy threads, Hikari pending, partner latency / in-flight.
3. `Hang Anatomy - Health and Admin`: admin and health behavior under contention.
4. `Hang Anatomy - Partner Resilience` and `Hang Anatomy - Final Reference`: controlled failure path after hardening.

Stronger incident presets:

```bash
make up
make partner-very-slow
DURATION=20 HEAVY_THREADS=12 ADMIN_THREADS=2 HEALTH_THREADS=2 RAMP_UP=3 make test-mixed-load
```

```bash
make up
make mode-db-pressure
DURATION=20 THREADS=24 RAMP_UP=3 make test-db-pressure-load
```

## Branch Guide

Planned branch progression:

- `demo/00-baseline`
- `demo/01-incident-observable`
- `demo/02-three-queues-dashboard`
- `demo/03-bulkhead-partner`
- `demo/04-health-readiness-fix`
- `demo/05-virtual-threads-experiment`
- `demo/06-timeouts-deadline`
- `demo/07-circuit-breaker`
- `demo/08-short-transactions`
- `demo/09-db-bulkhead`
- `demo/10-rate-limiter`
- `demo/11-final-reference`

The final reference on `main` should match `demo/11-final-reference`.

## Dashboards

Provisioned dashboards:

- `Hang Anatomy - Overview`
- `Hang Anatomy - Three Queues`
- `Hang Anatomy - Partner Resilience`
- `Hang Anatomy - Health and Admin`
- `Hang Anatomy - Virtual Threads Experiment`
- `Hang Anatomy - Final Reference`

Direct URLs can be printed with:

```bash
make dashboard-urls
```

## Troubleshooting

See:

- [docs/troubleshooting.md](/Users/proselyte/Documents/@proselyte/conf/jug-freeze-anatomy/docs/troubleshooting.md)

Typical issues:

- Docker memory too low
- host port conflicts on `3000`, `3100`, `3200`, `4317`, `4318`, `5432`, `8080`, `8089`, `9009`
- WireMock mode not applied
- no data yet in Grafana because the stack has just started
