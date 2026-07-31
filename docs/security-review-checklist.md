# Security Review Checklist

Use this checklist for changes that affect authentication, authorization, transfer state, external input, configuration, or deployment behavior.

## Identity and authorization

- [ ] Public and authenticated routes are intentionally separated.
- [ ] Role checks remain authoritative on the backend.
- [ ] Frontend visibility is not treated as an authorization control.
- [ ] New roles or scopes are documented in Keycloak configuration and tests.
- [ ] Logout, session expiry, and token refresh behavior remain bounded.
- [ ] No client secret is added to browser-delivered code.

## Service and data ownership

- [ ] The owning service validates every state-changing request.
- [ ] Cross-service data is accessed through an explicit contract or event.
- [ ] No new shared writable database path is introduced.
- [ ] Identifiers are validated before repository or query use.
- [ ] Pagination, filtering, and sorting inputs are bounded.

## Transfer correctness

- [ ] Idempotency behavior is defined for the new or changed operation.
- [ ] Duplicate requests and duplicate message delivery are tested.
- [ ] State transitions reject invalid ordering.
- [ ] Reservation, settlement, rejection, and compensation preserve invariants.
- [ ] Retry behavior cannot create duplicate money movement.
- [ ] Failure reasons do not expose internal secrets or sensitive configuration.

## Input and output handling

- [ ] Request bodies, path values, query parameters, and headers are validated.
- [ ] Error responses avoid stack traces and implementation details.
- [ ] Logs avoid credentials, tokens, secrets, and unnecessary payload data.
- [ ] CSV, JSON, and UI output encode untrusted content safely.
- [ ] File or URL handling, when introduced, restricts protocols, size, and destination.

## Configuration and secrets

- [ ] No real credentials or private keys are committed.
- [ ] Example values are clearly non-production.
- [ ] CI output and uploaded artifacts do not expose secrets.
- [ ] Security-sensitive defaults fail closed.
- [ ] Debug settings are not enabled silently in release or shared environments.

## Dependencies and infrastructure

- [ ] New dependencies have a clear purpose and supported version.
- [ ] Dependency advisories are reviewed for reachable impact.
- [ ] Container ports and administrative endpoints are not exposed unnecessarily.
- [ ] Health and metrics endpoints disclose only intended information.
- [ ] Database, Kafka, Redis, and Keycloak credentials remain environment-specific.

## Validation evidence

A security-relevant pull request should record:

- affected trust boundary;
- abuse or failure case considered;
- tests added or executed;
- configuration changes;
- known limitations;
- any follow-up requiring independent review.

This checklist improves engineering discipline but does not replace threat modeling, penetration testing, compliance review, or production security operations.
