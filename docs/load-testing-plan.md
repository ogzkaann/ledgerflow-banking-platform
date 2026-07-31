# Transfer Load-Testing Plan

This document defines a reproducible performance-testing scope for LedgerFlow. It is a test plan, not evidence that production capacity has been established.

## Objectives

Measure how the composed local or dedicated test environment behaves under controlled transfer traffic while preserving correctness.

Primary questions:

- How does transfer submission latency change as concurrency increases?
- What is the time from accepted request to terminal workflow state?
- Does idempotency remain correct under replay and retry pressure?
- Do Kafka lag, database pools, Redis operations, or service CPU become the first bottleneck?
- Do rejection and compensation paths preserve account consistency under load?

## Environment requirements

Use an isolated environment with:

- fixed service versions and configuration;
- dedicated PostgreSQL, Kafka, Redis, and Keycloak instances;
- fictional accounts and credentials;
- known resource limits;
- synchronized clocks;
- Prometheus metrics and correlation-capable logs;
- no developer workstation workloads competing for resources when publishing results.

Record hardware, container limits, JVM settings, database pool sizes, Kafka partitions, and test duration with every report.

## Proposed k6 scenarios

### Baseline read traffic

- authenticate with documented test users;
- list accounts;
- read account details;
- list transfers and read one transfer timeline.

Purpose: establish API and identity overhead before mutation traffic.

### Successful transfer flow

- select funded source and destination accounts;
- create a transfer with a unique idempotency key;
- poll with bounded backoff until terminal state;
- verify completion and record end-to-end duration.

### Idempotency replay

- submit the same request and key multiple times;
- confirm one logical transfer exists;
- record response consistency and duplicate-processing evidence.

### Mixed outcomes

Use deterministic fixtures for:

- successful settlement;
- risk rejection;
- insufficient funds;
- a documented compensation path.

Do not generate random failures that cannot be reproduced.

### Soak test

Run moderate sustained traffic long enough to observe:

- memory growth;
- connection leaks;
- Kafka lag accumulation;
- retry storms;
- log volume and disk growth.

## Metrics

Capture at minimum:

- request rate and error rate;
- HTTP p50, p95, and p99 latency;
- accepted-to-terminal p50, p95, and p99 duration;
- Kafka consumer lag;
- database connection pool utilization;
- JVM heap, GC pauses, CPU, and thread counts;
- Redis latency and error rate;
- count of duplicate logical transfers;
- count of inconsistent balances or non-terminal workflows.

Correctness failures fail the test even when latency thresholds pass.

## Initial thresholds

Thresholds should be treated as experiment gates, not production SLOs. Start with conservative values and revise only with recorded evidence.

Example categories:

- HTTP error rate below an explicit test threshold;
- no duplicate logical transfer for replayed idempotency keys;
- no unexplained non-terminal transfer after the defined timeout;
- zero balance invariant failures;
- bounded Kafka lag that returns to baseline after traffic stops.

## Reporting

Every report must include:

- commit SHA;
- environment description;
- scenario and data setup;
- k6 configuration;
- results and graphs;
- correctness checks;
- bottlenecks observed;
- invalid or interrupted runs;
- changes proposed for the next run.

Do not describe a laptop result as production capacity or extrapolate to customer volume without a representative environment.

## Implementation sequence

1. Add reusable fictional account fixtures.
2. Add k6 authentication helpers and configuration validation.
3. Implement baseline read and successful-transfer scenarios.
4. Add replay and mixed-outcome scenarios.
5. Export k6 and service metrics into one timestamped report.
6. Run a short CI smoke profile and keep heavier tests manual or scheduled.
7. Commit one baseline report only after the environment and run are reproducible.

Related roadmap: #8.
