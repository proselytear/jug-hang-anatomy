# Troubleshooting

## Port Conflicts

The demo binds:

- `3000` Grafana
- `3100` Loki
- `3200` Tempo HTTP
- `4317` Alloy OTLP gRPC
- `4318` Alloy OTLP HTTP
- `5432` PostgreSQL
- `8080` app
- `8089` WireMock
- `9009` Mimir

If `make up` fails early, check `lsof -i` or Docker Desktop and free the conflicting port.

## Grafana Has No Data

- Wait 10-30 seconds after startup.
- Check `make logs-infra`.
- Verify Alloy is healthy on `http://localhost:12345/-/ready`.
- Verify the app metric endpoint: `curl -fsS http://localhost:8080/actuator/prometheus`.

## Loki Has No Logs

- Verify the app is writing `/var/log/hang-anatomy/application.json.log`.
- Check Alloy logs for `loki.source.file` errors.
- Confirm the `app-logs` volume is mounted read-only into Alloy.

## Tempo Has No Traces

- Check `management.otlp.tracing.endpoint`.
- Verify Alloy listens on `4318`.
- Confirm the app can resolve `http://alloy:4318/v1/traces` inside Compose.

## Mimir Has No Metrics

- Verify Alloy scrape configuration for `app:8080/actuator/prometheus`.
- Check `prometheus.remote_write` errors in Alloy logs.
- Check that Mimir is healthy on `http://localhost:9009/ready`.

## WireMock Mode Does Not Switch

- Check `http://localhost:8089/__admin/health`.
- Re-run one of:
  `make partner-normal`
  `make partner-slow`
  `make partner-very-slow`
  `make partner-unstable`
- Check `GET /api/v1/demo/config` to confirm the app mode tag changed too.

## JMeter Cannot Connect

- The bundled runner uses `host.docker.internal:8080` by default.
- On Linux, override `BASE_URL`, for example:
  `BASE_URL=http://172.17.0.1:8080 make test-baseline-load`
- Generated results land in `load-tests/results/<scenario>/<timestamp>/`.

## Java 25 / Gradle Issues

- The wrapper is pinned to `Gradle 9.1.0` for Java 25 compatibility in this repo.
- Spring Boot 4 uses Jackson 3 package names. If a dependency still imports `com.fasterxml.jackson.*`, update it or pin a compatible version.

## Docker Runtime Image Choice

- A minimal Java 25 JRE image was not used here because availability and tooling support were less predictable than the Temurin JDK image in this environment.
- The runtime stage therefore uses `eclipse-temurin:25-jdk`. This is heavier, but stable for a conference demo.

## Docker Memory Requirements

- Allocate at least 6 GB to Docker Desktop for comfortable startup.
- Grafana + Mimir + Tempo + PostgreSQL + app + JMeter can be noisy on smaller limits.

## Health Checks Flap

- If `app` health flaps during boot, give PostgreSQL and Alloy time to become healthy.
- The compose file already uses `depends_on` with health conditions, but a cold image pull can still delay readiness.
