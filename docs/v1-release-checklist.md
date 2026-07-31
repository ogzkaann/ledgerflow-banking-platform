# LedgerFlow v1 Release Checklist

This checklist defines the portfolio release gate for `v1.0.0`. It does not certify the project for regulated production use.

## Repository state

- [ ] Default branch is clean and synchronized.
- [ ] Open pull requests are reviewed or intentionally deferred.
- [ ] Version references and README status describe Phases 0–5 consistently.
- [ ] Known limitations are current.
- [ ] No credentials, tokens, generated secrets, or personal data are committed.

## Backend validation

- [ ] Maven build completes from a clean checkout.
- [ ] Unit and integration suites pass.
- [ ] Testcontainers-backed service tests pass where supported.
- [ ] Database migrations apply to empty and supported upgrade paths.
- [ ] OpenAPI contracts are current.
- [ ] Generated clients match committed contracts.
- [ ] Idempotency and duplicate-delivery tests remain green.
- [ ] Compensation and rejection paths remain covered.

## Frontend validation

- [ ] Dependency installation succeeds from the lockfile.
- [ ] Type checking passes.
- [ ] Unit and accessibility checks pass.
- [ ] Production build succeeds.
- [ ] Operator, auditor, and admin behavior is verified.
- [ ] Session expiry and logout recovery remain functional.
- [ ] Empty, loading, success, rejected, and error states are readable.

## Composed-stack verification

- [ ] PostgreSQL, Kafka, Redis, Keycloak, services, and frontend start from documented commands.
- [ ] Demo realms, clients, roles, and users load correctly.
- [ ] A successful transfer reaches its terminal state.
- [ ] A rejected transfer exposes the reason.
- [ ] A compensated failure leaves account and transfer state consistent.
- [ ] Replaying an idempotency key does not create duplicate work.
- [ ] Notifications and transfer timelines match the workflow.
- [ ] Playwright completes against the real local stack.

## Observability

- [ ] Correlation identifiers are visible across the tested workflow.
- [ ] Health and readiness endpoints report expected state.
- [ ] Prometheus metrics are available.
- [ ] Grafana dashboards load with documented data sources.
- [ ] Failure-path logs are understandable without exposing secrets.

## Documentation

- [ ] Architecture overview matches implemented services and infrastructure.
- [ ] Local setup works from a clean environment.
- [ ] Demo credentials are explicitly non-production.
- [ ] Recruiter demo script is current.
- [ ] Production-readiness boundaries are linked.
- [ ] Screenshots and diagrams reflect the released UI.
- [ ] Phase 0–5 changes are summarized in release notes.

## Release procedure

1. Confirm all required CI workflows are green on the release commit.
2. Create an annotated `v1.0.0` tag.
3. Create a GitHub Release from the tag.
4. Include the delivered architecture, verification summary, known limitations, and demo instructions.
5. Link the release from the README and portfolio only after the tag is available.
6. Preserve failed or unavailable checks in the release record rather than presenting them as passes.

## Release note outline

- Account, transfer, risk, and notification service boundaries
- Kafka workflow, outbox, idempotency, reservation, settlement, and compensation
- Redis protective controls
- Keycloak OIDC and role-scoped operations
- React operations console
- OpenTelemetry, Prometheus, and Grafana
- Testcontainers and real-stack Playwright verification
- Explicit non-production boundaries
