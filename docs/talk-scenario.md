# Talk Scenario

This file is the step-by-step operator guide for the live demo.

## Baseline Incident

1. `git switch demo/00-baseline`
2. `make up`
3. `make partner-slow`
4. `make test-mixed-load`
5. Open `Hang Anatomy - Overview`
6. Show that `/api/v1/dashboard/{id}` p95 and p99 rise sharply.
7. Show that `/api/v1/admin/status` and `/actuator/health` degrade too.
8. Point out that CPU is not the main bottleneck signal.

## Three Queues

1. `git switch demo/02-three-queues-dashboard`
2. `make up`
3. `make partner-slow`
4. `make test-slow-partner-load`
5. Open `Hang Anatomy - Three Queues`
6. Walk the audience through:
   Tomcat busy/max
   Hikari active/pending
   partner latency, in-flight and timeouts
7. Explain the model: the service is a waiting system, not a CPU-bound system.

## Partner Bulkhead

1. `git switch demo/03-bulkhead-partner`
2. `make up`
3. `make partner-slow`
4. `make test-final-load`
5. Open `Hang Anatomy - Partner Resilience`
6. Show partner bulkhead rejections and controlled failures.
7. Compare admin and actuator stability against baseline.

## Health / Readiness Fix

1. `git switch demo/04-health-readiness-fix`
2. `make up`
3. `make partner-slow`
4. `make test-mixed-load`
5. Open `Hang Anatomy - Health and Admin`
6. Show readiness timeout/cache budget and liveness separation.
7. Explain why health checks must not amplify the incident.

## Virtual Threads Experiment

1. `git switch demo/05-virtual-threads-experiment`
2. `make up`
3. `make partner-slow`
4. Open `Hang Anatomy - Virtual Threads Experiment`
5. Restart the app with the VT profile if needed.
6. Show that VT may reduce thread waiting cost.
7. Show that VT do not create DB connections and do not speed up the partner API.

## Final Reference

1. `git switch demo/11-final-reference`
2. `make up`
3. `make partner-unstable`
4. `make test-final-load`
5. Open `Hang Anatomy - Final Reference`
6. Enumerate the treatment stack:
   observability baseline
   partner bulkhead
   correct readiness budget
   VT as an experiment, not a cure
   timeout and deadline
   circuit breaker
   short transactions
   DB bulkhead
   rate limiter
7. Close with the thesis:
   `Health = control of waiting queues`
