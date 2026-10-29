# Project Generation Prompt

```text
Ты senior staff-level engineer и solution architect.

Твоя задача: сгенерировать с нуля полноценный demo-репозиторий для conference talk на тему:

"Анатомия зависания: когда thread pool закончился, а CPU почти пустой"

Нужен production-like учебный проект, который показывает:
- saturation Tomcat worker threads
- starvation HikariCP
- медленный/нестабильный downstream HTTP dependency
- деградацию heavy endpoint
- деградацию operational endpoints (`/api/v1/admin/status`, `/actuator/health`, `/actuator/health/readiness`)
- эволюцию системы от плохого baseline к контролируемому final reference

Важно:
- проект должен быть не toy snippet, а целый репозиторий, который можно запустить локально
- все demo-состояния должны быть runnable
- каждый branch snapshot должен быть внутренне консистентным
- тесты в каждом branch snapshot должны проходить
- нельзя оставлять "мертвый" код из будущих этапов в baseline, если он ломает narrative или тесты

Сгенерируй проект со следующими требованиями.

==================================================
1. Stack
==================================================

Используй:
- Java 25
- Gradle Wrapper
- Spring Boot 4.x
- Spring MVC + embedded Tomcat
- JDBC + HikariCP
- PostgreSQL
- Flyway
- Micrometer + Actuator + Prometheus registry
- OpenTelemetry OTLP traces
- Resilience4j:
  - Bulkhead
  - CircuitBreaker
  - RateLimiter
  - TimeLimiter
- WireMock как partner API simulator
- Grafana
- Loki
- Mimir
- Tempo
- Grafana Alloy
- Docker Compose
- JMeter container runner

==================================================
2. Business shape
==================================================

Нужен один "тяжелый" endpoint:
- `GET /api/v1/dashboard/{id}`

Он должен:
1. читать данные из PostgreSQL
2. ходить во внешний partner API по HTTP
3. собирать единый DTO

Нужен легкий operational endpoint:
- `GET /api/v1/admin/status`

Нужны demo control endpoints:
- `POST /api/v1/demo/mode/{mode}`
- `GET /api/v1/demo/config`

Нужны Actuator endpoints:
- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/actuator/prometheus`
- `/actuator/info`

==================================================
3. Demo modes
==================================================

Сделай режимы:
- `normal`
- `slow-partner`
- `very-slow-partner`
- `unstable-partner`
- `db-pressure`
- `tx-hold`
- `recovery`

Они должны влиять на:
- поведение WireMock
- app-level mode tag в логах/метриках
- simulated delays, если это нужно для DB/admin/readiness paths

==================================================
4. Архитектурная эволюция по branch-ам
==================================================

Сделай branch progression:
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

Главные правила для веток:
- каждая ветка должна запускаться и проходить тесты
- ветка должна отражать именно тот этап narrative, который заявлен
- если две соседние ветки не отличаются по коду, это допустимо только если это осознанный operator checkpoint; это нужно явно задокументировать
- baseline должен быть намеренно плохим, но при этом честным и самосогласованным
- `main` должен совпадать с `demo/11-final-reference`

Смысл веток:

`demo/00-baseline`
- плохой пример
- long transaction
- HTTP внутри `@Transactional`
- нет timeout / circuit breaker / bulkhead / rate limiter
- admin/readiness без budget/caching

`demo/01-incident-observable`
- исправить самые грубые ошибки в control-plane
- вынести HTTP вне транзакции
- сделать observability и budgets видимыми

`demo/02-three-queues-dashboard`
- акцент на dashboards и модели "3 очереди"

`demo/03-bulkhead-partner`
- включить partner bulkhead

`demo/04-health-readiness-fix`
- readiness timeout/cache budget
- health checks не должны усугублять инцидент

`demo/05-virtual-threads-experiment`
- добавить профиль `vt`
- VT включаются не глобально, а через отдельный startup path
- показать, что VT помогают не во всем

`demo/06-timeouts-deadline`
- connect timeout
- read/response timeout
- overall request deadline

`demo/07-circuit-breaker`
- circuit breaker на partner

`demo/08-short-transactions`
- короткая транзакция только вокруг DB read path

`demo/09-db-bulkhead`
- DB bulkhead перед heavy DB-oriented read path

`demo/10-rate-limiter`
- inbound rate limiter для heavy endpoint

`demo/11-final-reference`
- все механизмы вместе

==================================================
5. Code design requirements
==================================================

Код должен быть чистым и понятным для live explanation.

Обязательно:
- разделить orchestration layer и transactional read layer
- heavy endpoint orchestration без длинной транзакции
- короткий `@Transactional(readOnly = true)` метод только на DB read service
- внешний partner call инкапсулировать в отдельный `PartnerClient`
- low-cardinality metrics contract
- route template metrics (`/api/v1/dashboard/{id}`, а не raw UUID path)

Нельзя:
- прямые HTTP calls из controllers
- прямые repository calls из controllers
- high-cardinality metrics tags (`dashboardId`, `customerId`, `requestId`)
- misleading names
- dead code, который не участвует в branch scenario

Сделай хорошие имена:
- `DashboardService`
- `DashboardReadService`
- `PartnerClient`
- `AdminStatusService`
- `CachedDbReadinessHealthIndicator`
- `RequestDeadline`
- `DependencyTimeoutException`
- `HeavyRequestRejectedException`

==================================================
6. Resilience requirements
==================================================

Нужны:

Partner side:
- semaphore bulkhead
- timeout / deadline
- circuit breaker
- fallback to cached snapshot if possible

DB side:
- short transactions
- DB bulkhead

Inbound side:
- rate limiter only for heavy endpoint

Control plane:
- admin endpoint with tight budget and stale-cache fallback
- readiness with timeout + cache
- liveness separated from readiness

==================================================
7. Observability requirements
==================================================

Сделай полноценную observability story.

Metrics:
- HTTP RED by route template
- Tomcat:
  - busy
  - current
  - max
- Hikari:
  - active
  - idle
  - pending
  - max
  - timeout_total
- Partner custom metrics:
  - duration
  - inflight
  - errors
  - timeouts
  - fallbacks
  - rejections
- Resilience4j tagged metrics for:
  - bulkhead
  - circuit breaker
  - rate limiter

Logs:
- JSON logs
- traceId
- spanId
- endpoint
- mode
- outcome
- durationMs

Traces:
- inbound HTTP span
- partner client span
- DB work when available

==================================================
8. Dashboards
==================================================

Сгенерируй provisioned Grafana dashboards:
- `Hang Anatomy - Overview`
- `Hang Anatomy - Three Queues`
- `Hang Anatomy - Partner Resilience`
- `Hang Anatomy - Health and Admin`
- `Hang Anatomy - Virtual Threads Experiment`
- `Hang Anatomy - Final Reference`

Идеи панелей:

Overview:
- RPS by endpoint
- error rate by endpoint
- p95 latency by endpoint
- p99 latency by endpoint
- CPU
- memory/heap
- restart count / process start

Three Queues:
- Tomcat thread pool: busy / current / max
- Tomcat saturation now
- Tomcat headroom now
- Hikari pool state: active / idle / pending / max
- Hikari active/max now
- Hikari pressure: pending + timeouts/5m
- Partner pressure: in-flight / bulkhead in use / bulkhead max
- Partner latency p95 / p99
- Partner failures: timeouts / errors / rejections

Partner Resilience:
- bulkhead concurrent calls
- bulkhead rejected calls
- circuit breaker state
- fallback count
- partner latency

Health and Admin:
- `/api/v1/admin/status` latency/errors
- `/actuator/health` latency/errors
- readiness failures

Virtual Threads Experiment:
- actuator latency before/after VT
- admin latency before/after VT
- Hikari pending
- Tomcat busy

Final Reference:
- p95 / p99
- errors
- Tomcat queue view
- Hikari pending
- partner in-flight
- bulkhead rejected
- circuit breaker state
- rate limiter rejected
- summary panel

==================================================
9. Infra and local run
==================================================

Нужен `docker-compose.yml` со службами:
- app
- postgres
- wiremock
- alloy
- loki
- mimir
- tempo
- grafana

Сделай так, чтобы:
- `make up` поднимал весь стек
- `make smoke` проверял основные endpoints
- `make dashboard-urls` печатал прямые ссылки на dashboards

Добавь `make` targets:
- `make build`
- `make test`
- `make up`
- `make up-vt`
- `make down`
- `make restart`
- `make partner-normal`
- `make partner-slow`
- `make partner-very-slow`
- `make partner-unstable`
- `make partner-recovery`
- `make mode-db-pressure`
- `make mode-tx-hold`
- `make test-baseline-load`
- `make test-mixed-load`
- `make test-slow-partner-load`
- `make test-db-pressure-load`
- `make test-final-load`
- `make demo-baseline`
- `make demo-bulkhead`
- `make demo-vt`
- `make demo-final`

==================================================
10. Load tests
==================================================

Добавь JMeter сценарии:
- heavy baseline load
- mixed heavy/admin/health load
- slow partner load
- db pressure load
- final resilience load

Сделай параметры через env variables:
- `THREADS`
- `HEAVY_THREADS`
- `ADMIN_THREADS`
- `HEALTH_THREADS`
- `DURATION`
- `RAMP_UP`

==================================================
11. Tests
==================================================

Нужны:
- unit tests
- integration tests
- observability integration tests
- architecture tests

Проверь как минимум:
- heavy endpoint happy path
- admin endpoint happy path
- baseline branch slow partner behavior
- timeout + fallback behavior on hardened branches
- low-cardinality metrics
- controllers do not call repositories directly
- external partner calls go only through `PartnerClient`

Критично:
- тесты должны соответствовать branch behavior
- нельзя переиспользовать финальные тестовые ожидания на baseline, если baseline специально ведет себя хуже

==================================================
12. Docs
==================================================

Сгенерируй:
- `README.md`
- `docs/architecture.md`
- `docs/observability.md`
- `docs/talk-scenario.md`
- `docs/troubleshooting.md`

В `docs/talk-scenario.md` обязательно:
- пошаговый operator flow по branches
- какие команды запускать
- какие dashboards открывать
- что именно объяснять на каждом этапе
- явное примечание, если некоторые ветки являются operator checkpoints, а не уникальными code diffs

==================================================
13. Quality bar
==================================================

Сгенерированный проект должен:
- собираться локально
- проходить тесты
- быть понятным для conference demo
- иметь чистую структуру пакетов
- иметь понятные имена классов, методов и переменных
- иметь минимально достаточные комментарии
- быть пригодным и для live demo, и для code slides

==================================================
14. Output format
==================================================

Сначала выдай:
1. краткую архитектурную концепцию
2. список файлов и директорий, которые создашь
3. branch progression и что именно меняется в каждой ветке

Затем сгенерируй содержимое проекта:
- build files
- application code
- tests
- infra configs
- dashboards
- scripts
- docs

Если проект слишком большой для одного ответа:
- разбей output на логические части
- но сохраняй полную согласованность между частями

Главная цель:
создать учебный production-like репозиторий, который наглядно показывает,
что "health = control of waiting queues", а не "CPU всегда виноват".
```
