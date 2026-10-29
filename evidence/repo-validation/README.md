# Проверка новой версии репозитория

Это проверка исправлений репозитория, а не новая нагрузочная серия статьи.
Исходный код проверен на коммите `223651907bcd6674f782b0a3ba0ba4e8d1227330`.
Позднейшие коммиты статьи могут добавлять текст и доказательства, не меняя этот код.

- `./gradlew test --no-daemon`: 25 тестов, без ошибок и пропусков.
- Первая попытка штатного `make up` собрала образ, но Docker не смог выделить
  сеть: все стандартные пулы адресов хоста были исчерпаны.
- После проверки пересечений выбран свободный диапазон `10.253.30.0/24`.
  Выполнен тот же `make up` с внешним `network-only.override.yml` через параметр
  `COMPOSE`. Приложение, параметры Spring и его YAML не переопределялись.
- Smoke-проверки прошли. В `startup.log` пул запускается с именем `hang-hikari`.
- HTTP 200 с `partner.source=snapshot` содержит `outcome=fallback-bulkhead`.
  `snapshot-log-selected.jsonl` содержит соответствующую запись JSON-журнала.
- В `prometheus-before.txt` и `prometheus-after.txt` присутствуют
  `executor_active_threads` и `executor_queued_tasks` для `partner-executor` и
  `budget-executor`.

Полная команда успешного запуска:

```bash
make up COMPOSE='docker compose -f docker-compose.yml -f /private/tmp/hang-v5-network.override.yml'
```

Внешний файл приведён рядом как `network-only.override.yml`. Его подсеть
при повторе следует выбирать с учётом сетей своего Docker-хоста.
Исходные сети и контейнеры других проектов не удалялись. После проверки режим
вернули к normal и остановили только проверочный проект, сохранив тома.
