SHELL := /bin/bash
.DEFAULT_GOAL := help

COMPOSE := docker compose
APP_URL := http://localhost:8080
GRAFANA_URL := http://localhost:3000
WIREMOCK_URL := http://localhost:8089

.PHONY: help clean clean-volumes build test up down restart logs logs-infra ps grafana smoke \
	partner-normal partner-slow partner-very-slow partner-unstable partner-recovery \
	test-baseline-load test-mixed-load test-slow-partner-load test-db-pressure-load test-final-load \
	demo-baseline demo-bulkhead demo-vt demo-final open-grafana dashboard-urls

help:
	@printf "%s\n" \
	"make build                Build bootJar with Gradle wrapper" \
	"make test                 Run unit, integration, observability and architecture tests" \
	"make up                   Start the full demo stack and run smoke checks" \
	"make down                 Stop the stack" \
	"make clean                Alias of down" \
	"make clean-volumes        Stop the stack and remove named volumes" \
	"make restart              Restart the demo application container" \
	"make logs                 Tail application logs" \
	"make logs-infra           Tail infrastructure logs" \
	"make ps                   Show docker compose processes" \
	"make grafana              Print Grafana URL and credentials" \
	"make smoke                Run stack smoke checks" \
	"make partner-normal       Switch WireMock to normal mode" \
	"make partner-slow         Switch WireMock to slow partner mode" \
	"make partner-very-slow    Switch WireMock to very slow partner mode" \
	"make partner-unstable     Switch WireMock to unstable partner mode" \
	"make partner-recovery     Switch WireMock back to recovery mode" \
	"make test-baseline-load   Run baseline heavy JMeter scenario" \
	"make test-mixed-load      Run mixed heavy/admin/health JMeter scenario" \
	"make test-slow-partner-load Run slow-partner JMeter scenario" \
	"make test-db-pressure-load Run DB pressure JMeter scenario" \
	"make test-final-load      Run final resilience JMeter scenario" \
	"make demo-baseline        Switch to baseline branch and prepare slow-partner demo" \
	"make demo-bulkhead        Switch to bulkhead branch and prepare demo" \
	"make demo-vt              Switch to virtual threads branch and prepare demo" \
	"make demo-final           Switch to final reference branch and prepare demo" \
	"make open-grafana         Open Grafana in browser if available" \
	"make dashboard-urls       Print direct dashboard URLs"

clean: down

clean-volumes:
	$(COMPOSE) down -v --remove-orphans

build:
	./gradlew clean bootJar

test:
	./gradlew test

up:
	$(COMPOSE) up -d --build
	./scripts/smoke/check-stack.sh

down:
	$(COMPOSE) down --remove-orphans

restart:
	$(COMPOSE) restart app

logs:
	$(COMPOSE) logs -f app

logs-infra:
	$(COMPOSE) logs -f grafana loki mimir tempo alloy wiremock postgres

ps:
	$(COMPOSE) ps

grafana:
	@echo "Grafana: $(GRAFANA_URL)"
	@echo "Username: admin"
	@echo "Password: admin"

smoke:
	./scripts/smoke/check-stack.sh

partner-normal:
	./scripts/wiremock/set-normal.sh

partner-slow:
	./scripts/wiremock/set-slow.sh

partner-very-slow:
	./scripts/wiremock/set-very-slow.sh

partner-unstable:
	./scripts/wiremock/set-unstable.sh

partner-recovery:
	./scripts/wiremock/reset.sh

test-baseline-load:
	./load-tests/scripts/run-jmeter.sh baseline-heavy-load

test-mixed-load:
	./load-tests/scripts/run-jmeter.sh admin-health-mixed-load

test-slow-partner-load:
	./load-tests/scripts/run-jmeter.sh partner-slow-load

test-db-pressure-load:
	./load-tests/scripts/run-jmeter.sh db-pressure-load

test-final-load:
	./load-tests/scripts/run-jmeter.sh final-resilience-load

demo-baseline:
	git switch demo/00-baseline
	$(MAKE) up
	$(MAKE) partner-slow

demo-bulkhead:
	git switch demo/03-bulkhead-partner
	$(MAKE) up
	$(MAKE) partner-slow

demo-vt:
	git switch demo/05-virtual-threads-experiment
	$(MAKE) up
	$(MAKE) partner-slow

demo-final:
	git switch demo/11-final-reference
	$(MAKE) up
	$(MAKE) partner-unstable

open-grafana:
	open $(GRAFANA_URL) || echo "$(GRAFANA_URL)"

dashboard-urls:
	./scripts/demo/dashboard-urls.sh
