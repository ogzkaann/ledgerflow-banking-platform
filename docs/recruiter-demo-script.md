# Five-Minute Recruiter Demo

This walkthrough is designed for a technical recruiter, hiring manager, or interview panel. It demonstrates the implemented system without implying production banking certification.

## Preparation

Before the call:

- start the documented local stack;
- confirm Keycloak, PostgreSQL, Kafka, Redis, backend services, and frontend are healthy;
- load only fictional demo data;
- open the operations console and observability view;
- keep the architecture diagram and repository README available.

## 0:00–0:40 — Problem and architecture

> LedgerFlow models a bank-transfer workflow where account ownership, transfer orchestration, risk decisions, and notifications are separate services. The main engineering problem is maintaining understandable state across asynchronous failure and retry boundaries.

Show the architecture overview and identify:

- account service;
- transfer service;
- risk service;
- notification service;
- PostgreSQL ownership;
- Kafka, Redis, and Keycloak.

## 0:40–1:20 — Identity and role scope

Sign in through Keycloak using a documented demo role.

Show:

- Authorization Code with PKCE login;
- role-aware navigation or actions;
- the absence of a frontend client secret;
- logout or session recovery behavior only if time allows.

Evidence: Keycloak configuration, frontend auth integration, and authorization tests.

## 1:20–2:25 — Successful transfer

1. Open or create two fictional accounts.
2. Submit a transfer with a visible idempotency key.
3. Follow the transfer timeline from pending through reservation and settlement to completion.
4. Show the resulting account balances and notification.

Explain that the browser request does not pretend the distributed workflow is synchronous. The terminal state is reached through durable asynchronous processing.

## 2:25–3:15 — Idempotency and duplicate protection

Repeat the same request using the same idempotency key.

Show that:

- a second transfer is not created;
- the response resolves to the original operation;
- duplicate Kafka delivery is handled safely by consumers;
- outbox publication protects the database-to-message boundary.

Evidence: integration tests and workflow records.

## 3:15–4:05 — Rejection or compensation

Run one documented failure scenario, such as a risk rejection or a failure after reservation.

Show:

- explicit rejected or compensated state;
- reason and timeline;
- account consistency after compensation;
- operator-visible evidence rather than a silent rollback claim.

## 4:05–4:40 — Operations evidence

Open the System or observability area.

Show:

- health/readiness state;
- correlation identifiers;
- representative metrics or traces;
- the boundary between application behavior and infrastructure monitoring.

## 4:40–5:00 — Honest scope statement

> The portfolio scope is complete through the operations console and real-stack verification. It does not integrate real payment rails, customer funds, KYC/AML vendors, multi-region infrastructure, or regulatory certification. Those boundaries are documented explicitly.

## Evidence map

| Claim | Evidence |
|---|---|
| Service-owned data | architecture and database configuration |
| Durable publication | transactional outbox implementation and tests |
| Duplicate safety | idempotency and consumer tests |
| Visible compensation | transfer state model, UI timeline, integration tests |
| Role-aware console | Keycloak mappings and frontend route/action checks |
| Real-stack behavior | Playwright and composed-stack verification |
| Non-production boundaries | production-readiness assessment |

## Fallback

If the composed stack is unavailable during an interview, do not improvise a successful live claim. Use verified screenshots, test output, the architecture diagram, and repository links, then state clearly which live step could not be demonstrated.
