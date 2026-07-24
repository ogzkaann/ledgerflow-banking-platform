# Phase 5 Verification Record

This record distinguishes completed automated checks from separate live-stack
browser evidence. Phase 5 is complete only when the real section contains
observed resource identifiers and outcomes.

## Automated frontend checks

Environment: Node.js 24.18.0 and npm 11.16.0 on Windows.

| Check | Result |
| --- | --- |
| `npm ci` / dependency audit | passed; zero known npm vulnerabilities |
| `npm run lint` | passed with zero warnings |
| `npm run typecheck` | passed after regenerating all three OpenAPI clients |
| `npm run test` | passed: 17 unit/component tests, including account-view axe and session-expiry recovery |
| `npm run build` | passed; route-level lazy chunks emitted |
| `npm run test:e2e` | passed: 7 Chromium journeys |

The mocked browser suite verifies the Keycloak redirect contains
`response_type=code` and `code_challenge_method=S256`, account creation/funding,
auditor restrictions, operator transfer creation, pending-to-completed polling,
completed and compensated/rejected timelines, notification navigation,
responsive navigation, keyboard activation, and no transfer-journey console
errors.

## Backend and configuration baseline

- the pre-change `.\mvnw.cmd clean verify` passed;
- the final Java 25 `.\mvnw.cmd spotless:apply` and
  `.\mvnw.cmd clean verify` passed all six reactor modules in 6 minutes
  44 seconds;
- focused Account, Transfer, and Notification integration suites passed after the
  read APIs and migrations were added;
- all updated OpenAPI contracts passed Redocly validation;
- default and observability Compose models validated;
- the Keycloak script passed Bash syntax validation in its pinned image.

## Real browser evidence

On 2026-07-24, Chromium completed the real Playwright project against Keycloak,
the Gateway, all four business services, Kafka, Redis, and four PostgreSQL
databases. It authenticated `operator`, `auditor`, and `admin` through the
browser flow, verified operator writes, auditor read-only controls and guarded
routes, performed an identity-provider logout, and then ran all four admin
scenarios. The admin ID/access token claims contained only
`ledgerflow-admin` from the explicitly scoped LedgerFlow roles and audience
`ledgerflow-api`; token values were neither printed nor attached.

| Scenario | Transfer ID | Correlation ID | Final status | Source final | Destination final | Ledger / notification evidence |
| --- | --- | --- | --- | ---: | ---: | --- |
| Successful transfer | `c8f13d38-8c86-4f5e-a1c8-d323b639b497` | `ee1d8555-2b09-450f-82b6-7bb24f8adab3` | `COMPLETED` | EUR 874.50, reserved 0.00 | EUR 225.50, reserved 0.00 | source/destination: 2 entries each (funding plus debit/credit); 1 completed notification |
| Risk rejection | `77f7cd94-082e-44e3-9d11-6d08c01711d8` | `cf4fc67d-16ce-4855-9235-ed5686b99183` | `REJECTED` after `COMPENSATING` | EUR 1,000.00, reserved 0.00 | EUR 100.00, reserved 0.00 | 1 funding entry per account, no transfer debit/credit; 1 rejected notification |
| Insufficient funds | `70f2d87f-8785-4dca-8c04-808261485212` | `cb7b3f03-b5f6-4e25-9c2a-abf5cee02141` | `REJECTED` | EUR 50.00, reserved 0.00 | EUR 100.00, reserved 0.00 | 1 funding entry per account, no balance mutation; 1 rejected notification |
| Idempotency replay | `c267466c-2214-4da2-9288-8505186dea69` | `322e8e61-922e-43df-ad38-9329ade4929c` | `COMPLETED` | EUR 874.50, reserved 0.00 | EUR 225.50, reserved 0.00 | replay header confirmed, same transfer ID, 2 entries per account, 1 completed notification |

The corresponding account pairs were:

- success: `349603f0-383d-4a9b-84bd-a3f9932ab274` →
  `fea53bd6-cae8-4187-ad71-e060cee41a1d`;
- compensated rejection: `06640066-407f-4222-9941-1595a52f2486` →
  `778dd972-b581-4ce1-93be-7e68ff1862b9`;
- insufficient funds: `cd86171c-0598-4f7c-9e77-96c4e1f76ab2` →
  `8a09ea20-153b-4136-81dc-5f8f712ecdab`;
- replay: `98eda497-e019-4132-909c-aad6527afd23` →
  `448d6811-4d51-48ae-a3d6-22b55bae3104`.

Playwright asserted the terminal balances and statuses through service-owned
HTTP read APIs; the ledger and notification counts above were also checked
directly in each owning PostgreSQL database. Representative screenshots and the
HTML report are generated under
`apps/operations-console/test-results` and
`apps/operations-console/playwright-report`; these ignored artifacts include
login, overview, account list/detail, transfer list, workflow processing,
completed/rejected transfer detail, notifications, Demo Lab, System, tablet,
and settled mobile navigation.

## Observability and safety evidence

The optional observability profile was started with all five applications using
`local,observability`. Prometheus reported all five scrape targets `up`.
During an instrumented four-scenario run, counters changed as follows:

| Metric | Before | After |
| --- | ---: | ---: |
| `transfer_accepted_total` | 0 | 4 |
| `transfer_completed_total` | 0 | 2 |
| `transfer_rejected_total` | 0 | 2 |
| `risk_rejected_total` | 0 | 1 |
| `notifications_recorded_total` | 0 | 4 |

Grafana 13 loaded the provisioned Overview, Transfer Workflow, and Kafka/Outbox
dashboards. Its Prometheus datasource proxy returned the same non-zero transfer
and notification counters.

A subsequent completed transfer,
`c267466c-2214-4da2-9288-8505186dea69`, produced 12 Elasticsearch/Kibana hits for
correlation `322e8e61-922e-43df-ad38-9329ade4929c`, covering Transfer, Account,
Risk, and Notification workflow events. A targeted scan of the current ECS JSON
logs found no JWT-shaped values or configured local password/client-secret
values.

Keyboard activation and the responsive drawer were exercised in Chromium.
Account-list automated axe analysis reported zero violations. Desktop, tablet,
and 390-pixel mobile screenshots were visually reviewed for clipping, UUID
wrapping, financial alignment, and settled navigation. A physical screen reader
was not used; semantic labels, live regions, focus visibility, reduced motion,
and heading/table structure were covered by code review and automated tests.

## CI evidence

Standard CI runs Maven verification and the complete fast frontend pipeline. The
security workflow scans committed history, analyzes Java and TypeScript with
CodeQL, audits npm, and publishes backend and frontend CycloneDX SBOMs. The
dedicated `Real browser verification` workflow starts four PostgreSQL databases,
Kafka, Redis, Keycloak, all five Java applications, and Chromium before running
the real Playwright project.
