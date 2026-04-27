# PRESENTATION_CONTEXT_FOR_CODEX.md

# Доклад: «Анатомия зависания»

## Назначение

Этот файл нужен Codex как read-only контекст по презентации.

Кодовая база уже существует и работает.  
Ничего в структуре проекта менять не нужно.

Codex должен помогать отвечать на вопросы вида:

- где код для слайда №XX;
- какой dashboard / panel в Grafana нужен для слайда №XX;
- как получить график для слайда №XX;
- какой JMeter / load script запускает сценарий для слайда №XX;
- где конфигурация Bulkhead / Circuit Breaker / Virtual Threads / Health Check;
- какие метрики использовать для конкретного слайда;
- какой фрагмент кода показать на слайде.

## Важные ограничения

1. Не менять текущую структуру проекта.
2. Не создавать новые файлы без явного запроса.
3. Не переименовывать существующие классы, пакеты, dashboards, scripts.
4. Не предлагать рефакторинг как ответ на вопрос “где код?”.
5. Сначала искать существующую реализацию.
6. Если точного файла нет — честно сказать, что не найдено, и указать ближайшие найденные места.
7. Любой ответ должен быть привязан к слайду презентации.

## Формат ответа Codex

На вопрос пользователя Codex должен отвечать так:

```text
Слайд: <номер и название>

Что нужно показать:
<коротко>

Где искать / найденные файлы:
- <path>
- <path>

Как запустить / получить график:
<команды, если есть>

Grafana:
- dashboard: <название или json-файл>
- panel: <название панели>
- query / metric: <PromQL или имя метрики>

Код для подсветки:
- <класс / метод / конфиг>
- <что именно подсветить>

Комментарий:
<важная оговорка, если есть>
```

Если точный файл не найден:

```text
Точный файл для слайда №XX не найден.

Ближайшие релевантные места:
- ...
- ...

Что нужно проверить вручную:
- ...
```

---

# 1. Главная идея презентации

Тема:

```text
Анатомия зависания

Когда thread pool закончился,
а CPU почти пустой
```

Главный тезис:

```text
Низкий CPU не означает, что система здорова.
Часто система не считает — она ждёт.
```

Центральная модель доклада:

```text
3 очереди ожидания:

1. Tomcat worker threads / backlog
2. HikariCP active / pending
3. Partner latency / in-flight / timeouts
```

Центральная метафора:

```text
Сервис как корабль.

Без отсеков:
heavy endpoint может утопить весь сервис.

С Bulkhead:
heavy endpoint может деградировать,
но admin и actuator остаются живыми.
```

---

# 2. Общая структура презентации

```text
Слайды 1–5:
Вход, автор, содержание.

Слайды 6–15:
Инцидент.
CPU почти пустой, но сервис висит.

Слайды 16–23:
Легенда сервиса.
Обычный Spring MVC сервис с 3 endpoint-ами.

Слайды 24–39:
Расследование.
RED, USE, путь запроса, 3 очереди: Tomcat / Hikari / Partner.

Слайды 40–51:
Спасаем корабль.
Bulkhead, health-check, actuator/admin должны выжить.

Слайды 52–58:
Virtual Threads.
Только после Bulkhead и health-check.

Слайды 59–72:
Лечение heavy endpoint.
Timeouts, Circuit Breaker, транзакции, DB bulkhead, rate limiter, финальный чеклист.
```

---

# 3. Главные endpoint-ы презентации

В презентации фигурируют 3 типа endpoint-ов.

```text
Heavy endpoint

/api/v1/dashboard/{id}

Смысл:
тяжёлый пользовательский endpoint.
Ходит в БД, вызывает внешнего партнёра, собирает DTO.
```

```text
Admin endpoint

/api/v1/admin/status

Смысл:
лёгкий служебный endpoint.
Должен оставаться доступным даже при деградации heavy.
```

```text
Actuator endpoint

/actuator/health
/actuator/prometheus

Смысл:
health-check и metrics endpoint.
Если они деградируют, система теряет управляемость.
```

При поиске в коде использовать ключи:

```bash
rg "dashboard"
rg "/api/v1/dashboard"
rg "admin/status"
rg "/api/v1/admin/status"
rg "actuator"
rg "health"
rg "prometheus"
```

---

# 4. Общие поисковые команды для Codex

## Поиск controller-ов

```bash
find . -type f | grep -Ei "Controller|Resource|Endpoint"
rg "@RestController|@Controller|@GetMapping|@RequestMapping"
rg "/api/v1/dashboard|/api/v1/admin/status"
```

## Поиск service layer

```bash
find . -type f | grep -Ei "Service|UseCase|Handler"
rg "DashboardService|AdminService|PartnerService"
```

## Поиск Hikari / datasource

```bash
rg "hikari|Hikari|maximum-pool-size|connection-timeout|datasource"
```

## Поиск Tomcat settings

```bash
rg "tomcat|threads|max|accept-count|server.tomcat"
```

## Поиск partner client

```bash
rg "Partner|partner|WebClient|RestClient|HttpClient|RestTemplate|Feign"
```

## Поиск Resilience4j / Bulkhead / Circuit Breaker

```bash
rg "resilience4j|Bulkhead|bulkhead|CircuitBreaker|circuitbreaker|TimeLimiter|RateLimiter"
```

## Поиск Virtual Threads

```bash
rg "virtual|Virtual|spring.threads.virtual|Executors.newVirtualThreadPerTaskExecutor"
```

## Поиск транзакций

```bash
rg "@Transactional|transaction"
```

## Поиск Grafana dashboards

```bash
find . -type f | grep -Ei "grafana|dashboard|json"
find . -type f -name "*.json" | grep -Ei "grafana|dashboard"
rg "http_server_requests|hikaricp|tomcat|partner|jvm|process.cpu|system.cpu"
```

## Поиск Prometheus / metrics

```bash
rg "prometheus|micrometer|MeterRegistry|Timer|Counter|Gauge|Observation"
rg "http.server.requests|http_server_requests"
rg "hikaricp.connections|tomcat.threads"
```

## Поиск JMeter / load scripts

```bash
find . -type f | grep -Ei "jmeter|jmx|load|stress|scenario"
find . -type f | grep -Ei "sh$|Makefile"
rg "jmeter|JMeter|dashboard|admin/status|actuator|bulkhead|virtual"
```

---

# 5. Слайды и артефакты

## Слайды 1–5. Вход

### Слайд 1. Титульный

Код не нужен.

Искать только assets презентации:

```bash
find . -type f | grep -Ei "logo|avatar|proselyte|png|svg"
```

---

### Слайд 2. Дисклеймер

Код не нужен.

---

### Слайд 3. Содержание

Код не нужен.

---

### Слайд 4. О себе

Код не нужен.

Возможные assets:

```bash
find . -type f | grep -Ei "avatar|photo|speaker|profile"
```

---

### Слайд 5. Сообщество

Код не нужен.

Возможные assets:

```bash
find . -type f | grep -Ei "telegram|youtube|qr|site|community"
```

---

# Блок 1. Инцидент

## Слайды 6–15

### Слайд 6. Инцидент

Код не нужен.

---

### Слайд 7. Как там прод?

Код не нужен.

---

### Слайд 8. Первый удар

Код не нужен.

---

### Слайд 9. Плохая комбинация условий

Что нужно показать:

```text
Normal traffic
→ Partner latency / DB pressure
→ Queue growth
→ Endpoint degradation
```

Искать:

```bash
rg "delay|fixedDelay|latency|slow|WireMock|partner"
find . -type f | grep -Ei "wiremock|mapping|partner|stub"
find . -type f | grep -Ei "jmx|load|incident"
```

Возможные артефакты:

```text
WireMock mapping с задержкой партнёра.
JMeter scenario для heavy load.
Load script для запуска инцидента.
```

---

### Слайд 10. Что случилось?

Что нужно показать:

```text
/api/v1/admin/status начал отвечать медленно или с ошибками.
```

Искать:

```bash
rg "/api/v1/admin/status|admin/status|AdminController"
rg "/api/v1/dashboard|DashboardController"
find . -type f | grep -Ei "incident|load|jmx"
```

---

### Слайд 11. RED: боль видна по endpoint-ам

Тип: Grafana.

Что нужно показать:

```text
p95/p99 latency:
- /api/v1/dashboard/{id}
- /api/v1/admin/status
- /actuator/health

errors/timeouts:
- 5xx
- timeouts
- connection errors
```

Искать dashboards:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|dashboard|red|incident|overview"
rg "http_server_requests|http.server.requests|p95|p99|histogram_quantile"
```

Метрики:

```text
http.server.requests
http_server_requests_seconds_count
http_server_requests_seconds_bucket
http_server_requests_seconds_sum
```

PromQL-кандидаты:

```promql
histogram_quantile(
  0.95,
  sum by (le, uri) (
    rate(http_server_requests_seconds_bucket[1m])
  )
)
```

```promql
histogram_quantile(
  0.99,
  sum by (le, uri) (
    rate(http_server_requests_seconds_bucket[1m])
  )
)
```

```promql
sum by (uri, status) (
  rate(http_server_requests_seconds_count{status=~"5.."}[1m])
)
```

Главное для ответа:

```text
Найти dashboard/panel, где одновременно видны heavy, admin и actuator.
```

---

### Слайд 12. CPU / Memory не объясняют аварию

Тип: Grafana.

Что нужно показать:

```text
CPU не 100%.
Memory без OOM.
Но endpoint-ы деградируют.
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|dashboard|jvm|system|cpu|memory|incident"
rg "process.cpu|system.cpu|jvm_memory|jvm.memory|memory"
```

Метрики:

```text
process.cpu.usage
system.cpu.usage
jvm.memory.used
jvm.memory.committed
jvm.memory.max
```

---

### Слайд 13. Главный разворот

Код не нужен.

Тезис:

```text
Низкий CPU не означает, что система здорова.
```

---

### Слайд 14. Первая гипотеза

Код не нужен.

Тезис:

```text
Система, скорее всего, не считает. Она ждёт.
```

---

### Слайд 15. Recap

Код не нужен.

---

# Блок 2. Легенда сервиса

## Слайды 16–23

### Слайд 16. Легенда

Код не нужен.

---

### Слайд 17. Сервис в 3 слоя

Что нужно показать:

```text
Transport → Business → Data / Integration
```

Искать:

```bash
find . -type f | grep -Ei "Controller|Service|Repository|Client"
rg "@RestController|@Service|Repository|JdbcTemplate|JpaRepository|WebClient|RestClient"
```

Артефакты:

```text
Controller classes.
Service classes.
Repository classes.
Partner client.
```

---

### Слайд 18. Что важно в этой модели

Что нужно показать:

```text
Transport держит поток.
Business оркестрирует.
Data / Integration рождает ожидание.
```

Искать те же файлы, что для слайда 17.

---

### Слайд 19. Три endpoint-а

Что нужно показать:

```text
Heavy:
/api/v1/dashboard/{id}

Admin:
/api/v1/admin/status

Actuator:
/actuator/health
/actuator/prometheus
```

Искать:

```bash
rg "/api/v1/dashboard|dashboard"
rg "/api/v1/admin/status|admin/status"
rg "management.endpoints|prometheus|health"
```

---

### Слайд 20. Контракты endpoint-ов

Тип: код.

Что нужно показать:

```text
Dashboard endpoint:
- DB read
- partner HTTP call
- DTO mapping

Admin endpoint:
- lightweight DB state

Actuator:
- health/prometheus
```

Искать:

```bash
rg "DashboardController|DashboardService|PartnerClient|AdminController"
rg "find|repository|partner|mapper|toDto"
```

Что подсветить:

```text
Controller method.
Service method.
DB call.
Partner call.
DTO mapping.
```

---

### Слайд 21. Скрытая связность

Что нужно показать:

```text
Endpoint-ы разные.
Ресурсы общие.
```

Искать:

```bash
rg "server.tomcat|tomcat"
rg "hikari|maximum-pool-size|datasource"
rg "PartnerClient|partner"
```

---

### Слайд 22. Почему это опасно

Что нужно показать:

```text
Heavy overload
→ shared resources saturation
→ Admin degraded
→ Actuator degraded
→ Restart loop / flapping
```

Искать:

```bash
find . -type f | grep -Ei "incident|load|jmx|grafana|dashboard"
rg "restart|readiness|liveness|health"
```

---

### Слайд 23. Recap

Код не нужен.

---

# Блок 3. Расследование: RED, USE и 3 очереди

## Слайды 24–39

### Слайд 24. Расследование

Код не нужен.

---

### Слайд 25. RED и USE

Что нужно показать:

```text
RED:
Requests, Errors, Duration

USE:
Utilization, Saturation, Errors
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|dashboard|red|use"
rg "RED|USE|Requests|Errors|Duration|Utilization|Saturation"
```

---

### Слайд 26. RED как инструмент расследования

Тип: Grafana / metrics.

Искать:

```bash
rg "http_server_requests|http.server.requests|histogram_quantile"
find . -type f -name "*.json" | grep -Ei "grafana|dashboard|red"
```

---

### Слайд 27. USE как инструмент расследования

Тип: Grafana / metrics.

Искать:

```bash
rg "tomcat.threads|hikaricp.connections|partner|timeout"
find . -type f -name "*.json" | grep -Ei "grafana|dashboard|use|queues"
```

---

### Слайд 28. Единственный слайд “Путь запроса”

Тип: Mermaid / pipeline / C4-like diagram.

Что нужно показать:

```text
Client
→ Tomcat
→ Controller
→ Service
→ Hikari / Postgres
→ Partner API
→ DTO Mapper
→ Client
```

Искать диаграмму:

```bash
find . -type f | grep -Ei "slide-28|request-path|path|mermaid|mmd|diagram"
```

Искать код:

```bash
rg "DashboardController|DashboardService|PartnerClient|Repository|Mapper"
```

Главная мысль:

```text
Tomcat worker thread занят на всём пути запроса.
Если поток ждёт Hikari, PostgreSQL или Partner API —
он всё равно занят.
```

---

### Слайд 29. Центральная модель доклада: 3 очереди

Тип: Grafana / diagram.

Что нужно показать:

```text
Request load
→ Tomcat worker threads / backlog
→ Hikari active / pending
→ Partner latency / in-flight
```

Искать:

```bash
find . -type f | grep -Ei "three-queues|queues|slide-29|grafana|dashboard|mmd"
rg "tomcat.threads|hikaricp.connections|partner"
```

---

### Слайд 30. Tomcat: первая очередь

Тип: схема + config.

Искать:

```bash
rg "server.tomcat|tomcat.threads|accept-count|max-connections|threads.max"
```

Метрики:

```text
tomcat.threads.busy
tomcat.threads.config.max
```

PromQL-кандидаты:

```promql
tomcat_threads_busy_threads
```

```promql
tomcat_threads_config_max_threads
```

Или искать фактические имена в `/actuator/prometheus`.

---

### Слайд 31. Hikari: вторая очередь

Тип: схема + config.

Искать:

```bash
rg "hikari|maximum-pool-size|connection-timeout|datasource"
```

Метрики:

```text
hikaricp.connections.active
hikaricp.connections.idle
hikaricp.connections.pending
hikaricp.connections.timeout
```

PromQL-кандидаты:

```promql
hikaricp_connections_active
```

```promql
hikaricp_connections_pending
```

```promql
hikaricp_connections_timeout_total
```

---

### Слайд 32. Partner: третья очередь

Тип: схема + metrics.

Искать:

```bash
rg "PartnerClient|partner|WebClient|RestClient|HttpClient|timeout|Timer|Observation"
```

Метрики-кандидаты:

```text
partner.call.duration
partner.call.errors
partner.call.timeout
partner.inflight
```

Важно:

```text
Фактические имена метрик нужно брать из кода или /actuator/prometheus.
Не выдумывать.
```

---

### Слайд 33. USE: Tomcat

Тип: Grafana.

Что нужно показать:

```text
busy threads приближаются к max,
latency endpoint-ов растёт.
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|tomcat|dashboard|queues"
rg "tomcat_threads|tomcat.threads"
```

---

### Слайд 34. USE: Hikari

Тип: Grafana.

Что нужно показать:

```text
active близко к max,
pending растёт,
connectionTimeout появляется или приближается.
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|hikari|dashboard|queues"
rg "hikaricp_connections|hikaricp.connections"
```

---

### Слайд 35. USE: Partner

Тип: Grafana.

Что нужно показать:

```text
partner latency растёт,
timeouts/errors растут,
это коррелирует с Tomcat busy и/или Hikari pending.
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|partner|dashboard|queues"
rg "partner|timeout|duration|Timer|Observation"
```

---

### Слайд 36. Thread dump

Тип: thread dump.

Что нужно показать:

```text
WAITING / TIMED_WAITING на getConnection()
TIMED_WAITING на socket read / HTTP client
BLOCKED на lock
```

Искать scripts:

```bash
find . -type f | grep -Ei "thread|dump|jstack|jcmd"
rg "jstack|jcmd|Thread.dump|threaddump"
```

Искать в dump:

```text
com.zaxxer.hikari.pool.HikariPool.getConnection
java.net.SocketInputStream.socketRead
org.apache.hc.client5
java.lang.Thread.State: BLOCKED
```

---

### Слайд 37. Почему p95/p99

Тип: Grafana / explanation.

Искать:

```bash
rg "histogram_quantile|0.95|0.99|p95|p99"
find . -type f -name "*.json" | grep -Ei "grafana|latency|dashboard"
```

PromQL:

```promql
histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket[1m])))
```

```promql
histogram_quantile(0.99, sum by (le, uri) (rate(http_server_requests_seconds_bucket[1m])))
```

---

### Слайд 38. Как читать корреляцию

Тип: table + Grafana references.

Что нужно показать:

```text
Tomcat busy растёт + partner latency растёт
→ downstream bottleneck.

Hikari pending растёт + partner latency не растёт
→ DB/lock/connection issue.

CPU низкий + очереди растут
→ система ждёт.
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|three|queues|correlation|overview"
rg "tomcat_threads|hikaricp_connections|partner|cpu"
```

---

### Слайд 39. Recap

Код не нужен.

---

# Блок 4. Спасаем корабль

## Слайды 40–51

### Слайд 40. Спасаем корабль

Код не нужен.

Возможный asset:

```bash
find . -type f | grep -Ei "ship|bulkhead|boat|png|jpg"
```

---

### Слайд 41. Главный принцип

Код не нужен.

Тезис:

```text
Admin и actuator должны жить даже тогда, когда heavy горит.
```

---

### Слайд 42. Почему тонет весь корабль

Тип: image / metaphor.

Что нужно показать:

```text
Корабль без отсеков.
Heavy endpoint даёт протечку.
Вода распространяется по всему корпусу.
Admin и Actuator тоже тонут.
```

Искать asset:

```bash
find . -type f | grep -Ei "ship|boat|no-bulkhead|without|hull|failure|png|jpg"
```

Код не нужен.

---

### Слайд 43. Bulkhead

Тип: image / metaphor.

Что нужно показать:

```text
Корабль с отсеками.
Heavy отсек повреждён.
Admin и Actuator остаются живыми.
```

Искать asset:

```bash
find . -type f | grep -Ei "ship|boat|bulkhead|compartment|png|jpg"
```

Код:

```bash
rg "Bulkhead|bulkhead|maxConcurrentCalls|maxWaitDuration"
```

---

### Слайд 44. Что решает Bulkhead

Тип: explanation.

Искать код:

```bash
rg "Bulkhead|bulkhead|maxConcurrentCalls|maxWaitDuration|resilience4j"
```

---

### Слайд 45. Какой Bulkhead используем

Тип: explanation / config.

Искать:

```bash
rg "SemaphoreBulkhead|ThreadPoolBulkhead|Bulkhead.Type|type: SEMAPHORE|semaphore|thread-pool"
rg "resilience4j.bulkhead"
```

Важно:

```text
Нужно определить по реальному коду, какой Bulkhead используется:
semaphore или thread-pool.
Не выдумывать.
```

---

### Слайд 46. Конфиг Bulkhead

Тип: code.

Что нужно показать:

```text
maxConcurrentCalls
maxWaitDuration
fallback / rejected path
```

Искать:

```bash
rg "resilience4j.bulkhead|bulkhead:"
rg "maxConcurrentCalls|max-wait-duration|maxWaitDuration"
rg "@Bulkhead|BulkheadRegistry|BulkheadConfig"
```

Возможные места:

```text
application.yml
application-*.yml
ResilienceConfig.java
PartnerClient.java
PartnerService.java
```

---

### Слайд 47. Эффект Bulkhead: actuator живёт

Тип: Grafana before/after.

Что нужно показать:

```text
До:
- /actuator/health latency растёт
- readiness fails
- restart / flapping

После:
- /actuator/health стабилен
- readiness отвечает предсказуемо
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|bulkhead|health|actuator|dashboard"
rg "actuator|health|readiness|bulkhead"
```

PromQL-кандидаты:

```promql
histogram_quantile(
  0.95,
  sum by (le, uri) (
    rate(http_server_requests_seconds_bucket{uri=~".*health.*"}[1m])
  )
)
```

Фактический `uri` нужно проверить в метриках.

---

### Слайд 48. Эффект Bulkhead: admin стал предсказуем

Тип: Grafana before/after.

Что нужно показать:

```text
До:
- /api/v1/admin/status p95/p99 растёт
- ошибки/таймауты

После:
- latency ниже и стабильнее
- heavy может деградировать,
  но admin больше не тонет вместе с ним
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|bulkhead|admin|dashboard"
rg "admin/status|AdminController|bulkhead"
```

---

### Слайд 49. Health-check может усилить аварию

Тип: explanation + code/config.

Что нужно показать:

```text
readiness ходит в БД
→ конкурирует за Hikari
→ увеличивает pending
```

Искать:

```bash
rg "HealthIndicator|Readiness|Liveness|readiness|liveness|management.endpoint.health"
rg "DataSourceHealthIndicator|db"
```

---

### Слайд 50. Правильный health

Тип: code/config.

Что нужно показать:

```text
liveness != readiness
DB-check не должен добивать систему
```

Искать:

```bash
rg "management.endpoint.health|management.health|liveness|readiness|HealthIndicator"
```

Возможные места:

```text
application.yml
application-*.yml
Custom HealthIndicator class
```

---

### Слайд 51. Recap

Код не нужен.

---

# Блок 5. Virtual Threads

## Слайды 52–58

### Слайд 52. Virtual Threads

Код не нужен.

---

### Слайд 53. Почему VT не раньше

Код не нужен.

Тезис:

```text
Сначала лимиты и изоляция.
Потом удешевление ожидания.
```

---

### Слайд 54. VT: обещание и реальность

Код не нужен.

---

### Слайд 55. Включаем Virtual Threads

Тип: code/config.

Что нужно показать:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Искать:

```bash
rg "spring.threads.virtual|virtual.enabled|VirtualThread|newVirtualThreadPerTaskExecutor"
```

Возможные места:

```text
application.yml
application-vt.yml
application-*.yml
Executor config class
```

---

### Слайд 56. VT помогли actuator

Тип: Grafana.

Что нужно показать:

```text
/actuator/health стал отвечать быстрее/стабильнее.
readiness failures снизились.
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|virtual|vt|actuator|health"
rg "virtual|actuator|health|readiness"
```

---

### Слайд 57. VT не вылечили admin

Тип: Grafana.

Что нужно показать:

```text
Если endpoint ждёт connection или downstream,
VT не устраняют корневую причину.
```

Искать:

```bash
find . -type f -name "*.json" | grep -Ei "grafana|virtual|vt|admin|hikari|partner"
rg "virtual|admin/status|hikaricp|partner"
```

---

### Слайд 58. Recap

Код не нужен.

---

# Блок 6. Лечим heavy endpoint

## Слайды 59–72

### Слайд 59. Лечим heavy endpoint

Код не нужен.

---

### Слайд 60. Порядок лечения

Код не нужен, но можно ссылаться на следующие слайды.

---

### Слайд 61. Time budget

Код не нужен.

Искать возможную реализацию deadline:

```bash
rg "deadline|time budget|timeout|Duration"
```

---

### Слайд 62. Timeouts

Тип: explanation / config.

Искать:

```bash
rg "connectTimeout|responseTimeout|readTimeout|connectionRequestTimeout|timeout|Duration"
rg "HttpClient|WebClient|RestClient|RestTemplate|Apache|OkHttp"
```

---

### Слайд 63. HTTP timeouts + deadline

Тип: code.

Что нужно показать:

```text
connect timeout
read/response timeout
pool acquisition timeout
overall deadline
```

Искать:

```bash
rg "connectTimeout|responseTimeout|readTimeout|connectionRequestTimeout|timeout"
rg "HttpClient|WebClient|RestClient|RestTemplate|ClientHttpRequestFactory"
```

Возможные места:

```text
HttpClientConfig.java
PartnerClient.java
application.yml
application-*.yml
```

---

### Слайд 64. Circuit Breaker

Код не обязателен, но искать:

```bash
rg "CircuitBreaker|circuitbreaker|resilience4j.circuitbreaker"
```

---

### Слайд 65. Что решает Circuit Breaker

Код не обязателен, но искать:

```bash
rg "CircuitBreaker|fallback|CallNotPermittedException"
```

---

### Слайд 66. Логика Circuit Breaker

Тип: diagram / config.

Искать:

```bash
rg "slidingWindow|failureRateThreshold|slowCallRateThreshold|waitDurationInOpenState|permittedNumberOfCallsInHalfOpenState"
rg "resilience4j.circuitbreaker"
```

---

### Слайд 67. Bulkhead vs Circuit Breaker

Тип: explanation.

Искать оба блока:

```bash
rg "Bulkhead|CircuitBreaker|bulkhead|circuitbreaker"
```

---

### Слайд 68. Длинная транзакция

Тип: explanation.

Искать:

```bash
rg "@Transactional|PartnerClient|partner|load|DashboardService"
```

---

### Слайд 69. Почему это опасно

Тип: code/explanation.

Что нужно показать:

```text
Connection удерживается во время ожидания внешнего HTTP.
Hikari pending растёт.
Tomcat threads заняты ожиданием.
```

Искать:

```bash
rg "@Transactional"
rg "partnerClient|PartnerClient|WebClient|RestClient|HttpClient"
```

Важно:

```text
Нужно найти метод, где @Transactional находится вокруг логики,
внутри которой есть внешний HTTP-вызов.
Если такого кода уже нет, указать, что плохой пример отсутствует в текущем коде.
```

---

### Слайд 70. Плохой пример транзакции

Тип: code.

Что нужно показать:

```java
@Transactional
public DashboardDto getDashboard(UUID id) {
    var data = repository.findData(id);

    var partnerData = partnerClient.load(id); // HTTP внутри Tx

    return mapper.toDto(data, partnerData);
}
```

Искать реальный аналог:

```bash
rg "@Transactional"
rg "partnerClient|PartnerClient|loadPartner|callPartner"
rg "DashboardService|getDashboard"
```

Если плохой пример в коде уже исправлен:

```text
Сказать: текущий код уже не содержит плохой реализации.
Для слайда можно использовать исторический пример / demo branch / snippet из docs.
```

---

### Слайд 71. Хороший пример транзакции

Тип: code.

Что нужно показать:

```text
Короткий @Transactional метод.
HTTP-вызов вне транзакции.
```

Искать:

```bash
rg "@Transactional\\(readOnly = true\\)|@Transactional"
rg "loadData|findData|repository"
rg "partnerClient|PartnerClient"
```

Что подсветить:

```text
Метод с @Transactional(readOnly = true) только для DB read.
Метод с partnerClient.load(...) находится вне транзакции.
```

---

### Слайд 72. Финальный чеклист

Код не нужен.

Но Codex может собрать ссылки на артефакты:

```text
Grafana dashboard “3 очереди”
Timeouts config
Bulkhead config
Circuit Breaker config
Transactional fix
DB bulkhead
Rate limiter
Virtual Threads config
```

Искать:

```bash
rg "tomcat|hikari|partner|timeout|Bulkhead|CircuitBreaker|RateLimiter|virtual|Transactional"
find . -type f -name "*.json" | grep -Ei "grafana|dashboard"
```

---

# 6. Ключевые Grafana-графики по слайдам

## Инцидент

Слайды:

```text
11, 12
```

Нужны графики:

```text
RED по endpoint-ам:
- request rate
- error rate
- p95/p99 latency

CPU / Memory:
- process CPU
- system CPU
- JVM memory
```

---

## 3 очереди

Слайды:

```text
33, 34, 35, 38
```

Нужны графики:

```text
Tomcat:
- busy threads
- max threads
- busy/max ratio

Hikari:
- active
- idle
- pending
- timeouts

Partner:
- p95/p99 latency
- errors
- timeouts
- in-flight, если есть
```

---

## Bulkhead

Слайды:

```text
47, 48
```

Нужны графики before/after:

```text
Actuator:
- /actuator/health p95/p99
- readiness failures
- restart/flapping, если есть

Admin:
- /api/v1/admin/status p95/p99
- errors/timeouts
```

---

## Virtual Threads

Слайды:

```text
56, 57
```

Нужны графики:

```text
Actuator до/после VT.
Admin до/после VT.
Hikari pending.
Partner latency.
```

---

## Heavy treatment

Слайды:

```text
63, 66, 70, 71
```

Нужны:

```text
Timeout config/code.
Circuit Breaker config/code.
Bad transaction example.
Good transaction example.
```

---

# 7. Ключевые кодовые темы по слайдам

## Endpoint contracts

Слайды:

```text
19, 20
```

Искать:

```bash
rg "DashboardController|AdminController|Health"
```

---

## Request path

Слайд:

```text
28
```

Искать:

```bash
rg "DashboardController|DashboardService|PartnerClient|Repository|Mapper"
find . -type f | grep -Ei "slide-28|request-path|mmd|diagram"
```

---

## Tomcat

Слайды:

```text
30, 33
```

Искать:

```bash
rg "server.tomcat|tomcat.threads|accept-count"
```

---

## Hikari

Слайды:

```text
31, 34
```

Искать:

```bash
rg "hikari|maximum-pool-size|connection-timeout"
```

---

## Partner latency

Слайды:

```text
32, 35
```

Искать:

```bash
rg "PartnerClient|partner|delay|timeout|WireMock"
```

---

## Bulkhead

Слайды:

```text
43–48
```

Искать:

```bash
rg "Bulkhead|bulkhead|maxConcurrentCalls|maxWaitDuration|resilience4j"
```

---

## Health-check

Слайды:

```text
49, 50
```

Искать:

```bash
rg "HealthIndicator|liveness|readiness|management.endpoint.health"
```

---

## Virtual Threads

Слайды:

```text
55–57
```

Искать:

```bash
rg "spring.threads.virtual|VirtualThread|virtual"
```

---

## Timeouts

Слайды:

```text
61–63
```

Искать:

```bash
rg "connectTimeout|responseTimeout|readTimeout|connectionRequestTimeout|timeout|deadline"
```

---

## Circuit Breaker

Слайды:

```text
64–67
```

Искать:

```bash
rg "CircuitBreaker|circuitbreaker|failureRateThreshold|slowCallRateThreshold|CallNotPermittedException"
```

---

## Transactions

Слайды:

```text
68–71
```

Искать:

```bash
rg "@Transactional|transaction|partnerClient|PartnerClient|DashboardService"
```

---

# 8. Как отвечать на вопрос “как получить график для слайда №XX”

Codex должен:

1. Определить slide number.
2. Найти тип графика по этому файлу.
3. Найти dashboard JSON или provisioning.
4. Найти PromQL внутри dashboard.
5. Найти сценарий нагрузки, который создаёт нужную картину.
6. Дать команды запуска, если они есть в README/Makefile/scripts.
7. Не придумывать команды, которых нет.

Пример ответа:

```text
Слайд 34: USE: Hikari pool

Что нужно показать:
active близко к max, pending растёт, connectionTimeout появляется.

Найденные dashboard-файлы:
- infrastructure/grafana/dashboards/three-queues.json

Панели:
- Hikari active connections
- Hikari pending connections
- Hikari connection timeouts

PromQL:
- hikaricp_connections_active
- hikaricp_connections_pending
- hikaricp_connections_timeout_total

Сценарий нагрузки:
- jmeter/incident-heavy-load.jmx
- scripts/load-incident.sh

Команда:
- <команда из Makefile/README, если есть>

Код/конфиг:
- services/.../application.yml
  параметры hikari.maximum-pool-size и hikari.connection-timeout
```

---

# 9. Как отвечать на вопрос “где код для слайда №XX”

Codex должен:

1. Определить slide number.
2. Найти связанную тему.
3. Сделать поиск по ключевым словам.
4. Вернуть конкретные файлы и методы.
5. Указать, какие строки или блоки подсветить.
6. Если слайд визуальный или текстовый — сказать, что кода нет.

Пример:

```text
Слайд 46: Конфиг Bulkhead

Искать / найдено:
- services/core-api/src/main/resources/application.yml
- services/core-api/src/main/java/.../PartnerClient.java

Что подсветить:
- resilience4j.bulkhead.instances.partnerApi.maxConcurrentCalls
- resilience4j.bulkhead.instances.partnerApi.maxWaitDuration
- @Bulkhead(name = "partnerApi")

Смысл для слайда:
Bulkhead ограничивает in-flight вызовы к партнёру и не даёт heavy endpoint размножать ожидание.
```

---

# 10. Смысловая последовательность demo

Codex должен понимать, что презентация идёт не от технологии к технологии, а по драматургии инцидента.

```text
1. Сначала показываем аварию.
2. Потом показываем, что CPU не объясняет проблему.
3. Потом вводим гипотезу: система ждёт.
4. Потом объясняем сервис и endpoint-ы.
5. Потом один раз показываем путь запроса.
6. Потом вводим 3 очереди.
7. Потом через RED/USE ищем, где копится ожидание.
8. Потом спасаем управляемость через Bulkhead и health-check.
9. Потом проверяем Virtual Threads.
10. Потом лечим heavy endpoint фундаментально.
```

Важно:

```text
Virtual Threads не должны подаваться как первое лечение.
Bulkhead и health-check идут раньше.
```

---

# 11. Частые вопросы пользователя и ожидаемые ответы

## “Где код для слайда 28?”

Ответ должен вести к:

```text
DashboardController
DashboardService
Repository
PartnerClient
DTO Mapper
request-path diagram
```

---

## “Как получить график для слайда 34?”

Ответ должен вести к:

```text
Grafana dashboard по Hikari
hikaricp_connections_active
hikaricp_connections_pending
hikaricp_connections_timeout_total
load scenario, который создаёт pending
```

---

## “Где Bulkhead?”

Ответ должен вести к:

```text
resilience4j.bulkhead config
@Bulkhead или programmatic Bulkhead usage
PartnerClient / PartnerService
fallback / rejected path
```

---

## “Где Virtual Threads?”

Ответ должен вести к:

```text
spring.threads.virtual.enabled=true
application profile
load scenario для сравнения before/after
dashboard с actuator/admin latency
```

---

## “Где плохая транзакция?”

Ответ должен:

```text
Искать @Transactional + partner HTTP call внутри одного метода.

Если такого кода уже нет:
сказать, что в текущей реализации плохой пример отсутствует,
и предложить использовать историческую ветку / snippet / docs.
```

---

## “Как показать, что CPU низкий?”

Ответ должен вести к:

```text
Grafana CPU panel
process.cpu.usage
system.cpu.usage
JVM memory рядом
RED latency/errors рядом
```

---

## “Как показать, что проблема в ожидании?”

Ответ должен вести к:

```text
Tomcat busy/max
Hikari pending
Partner latency
Thread dump
p95/p99
```

---

# 12. Запрещённые ответы

Codex не должен отвечать так:

```text
“Можно добавить новый dashboard...”
```

если пользователь спросил, где существующий график.

Не должен отвечать:

```text
“Давайте перепишем сервис...”
```

если вопрос был про код для слайда.

Не должен выдумывать:

```text
несуществующие ветки,
несуществующие файлы,
несуществующие dashboard names,
несуществующие PromQL-запросы как факт.
```

Допустимо:

```text
“Я не нашёл точный dashboard для слайда 47.
Ближайший найденный dashboard: ...
В нём есть панели ...
Для слайда не хватает панели ...
”
```

---

# 13. Минимальная карта slide → topic

```text
1  Title
2  Disclaimer
3  Agenda
4  About
5  Community

6  Incident section
7  Production seems normal
8  Bad combination
9  Conditions causing degradation
10 Admin degraded
11 RED endpoint pain Grafana
12 CPU/Memory Grafana
13 Low CPU thesis
14 Waiting hypothesis
15 Incident recap

16 Service legend section
17 3-layer service diagram
18 Important parts of model
19 Three endpoints
20 Endpoint contracts code
21 Shared resources
22 Shared resource danger
23 Legend recap

24 Investigation section
25 RED/USE
26 RED investigation
27 USE investigation
28 Request path diagram/code
29 Three queues model
30 Tomcat queue
31 Hikari queue
32 Partner queue
33 Tomcat Grafana
34 Hikari Grafana
35 Partner Grafana
36 Thread dump
37 p95/p99
38 Correlation table
39 Investigation recap

40 Ship rescue section
41 Admin/actuator must live
42 Ship without compartments
43 Bulkhead ship
44 Bulkhead meaning
45 Bulkhead type
46 Bulkhead config/code
47 Bulkhead effect actuator Grafana
48 Bulkhead effect admin Grafana
49 Health-check can amplify incident
50 Correct health code/config
51 Rescue recap

52 Virtual Threads section
53 Why VT not first
54 VT promise/reality
55 Enable VT config
56 VT helped actuator Grafana
57 VT did not fix admin Grafana
58 VT recap

59 Heavy treatment section
60 Treatment order
61 Time budget
62 Timeouts
63 HTTP timeouts code
64 Circuit Breaker section
65 Circuit Breaker meaning
66 Circuit Breaker states/config
67 Bulkhead vs Circuit Breaker
68 Long transaction section
69 Why long transaction is dangerous
70 Bad transaction code
71 Good transaction code
72 Final checklist
```
