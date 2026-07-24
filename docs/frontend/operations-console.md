# Operations Console

The Phase 5 console is a browser client under `apps/operations-console`. It is an
internal demonstration and operations surface, not a customer banking portal. It
uses only API Gateway routes and never connects directly to a service database or
to Risk Service.

## Stack and structure

- React 19.2.8, strict TypeScript 5.9.3, Vite 8.1.5, and React Router 7;
- TanStack Query for bounded server-state caching and cancellation;
- `oidc-client-ts` through `react-oidc-context` for Authorization Code + S256 PKCE;
- `openapi-typescript` and `openapi-fetch` for generated contract types and calls;
- Vitest, Testing Library, MSW, jest-axe, and Playwright;
- responsive CSS with semantic HTML, visible focus, AA-oriented colors,
  reduced-motion handling, and no external runtime font dependency.

The generated files in `src/api/generated` come from the Account, Transfer, and
Notification OpenAPI contracts:

```powershell
cd apps/operations-console
npm run generate:api
```

`src/api/clients.ts` is the transport boundary. It attaches the bearer token and a
correlation ID, applies a ten-second timeout, parses problem responses, dispatches
session expiry on `401`, and preserves `Retry-After`. GET queries retry at most
twice for transient failures. Writes never retry automatically.

## Routes and roles

| Route | Purpose | Roles |
| --- | --- | --- |
| `/` | Real counts, activity, and workflow summary | operator, auditor, admin |
| `/accounts` | Paginated/filterable accounts | operator, auditor, admin |
| `/accounts/new` | Create an account | operator, admin |
| `/accounts/:id` | Balances, metadata, ledger, optional demo funding | read: all; funding: admin |
| `/transfers` | Paginated/filterable transfers | operator, auditor, admin |
| `/transfers/new` | Create a transfer | operator, admin |
| `/transfers/:id` | Polling, actual history, notifications, replay proof | operator, auditor, admin |
| `/notifications` | Persisted terminal demo records | operator, auditor, admin |
| `/demo` | Four guided real-workflow scenarios | admin |
| `/system` | Readiness, architecture, security, and technical links | operator, auditor, admin |

Navigation and route guards are usability controls. Gateway and service
authorization remain authoritative; no security claim depends on a hidden button.

## Browser authentication

The public `ledgerflow-spa` client uses `response_type=code`, PKCE, no client
secret, and the existing realm issuer. OIDC state and the user session are stored
in `sessionStorage`; no token is written to persistent `localStorage`, printed,
or rendered. The callback accepts only an application-relative return path.
Library-supported session monitoring and silent renewal are enabled. Logout is an
identity-provider logout.

The idempotent `keycloak-demo-users` Compose task creates local `operator`,
`auditor`, and `admin` users from environment-supplied passwords. These users are
for local demonstration only.

## Money, idempotency, and polling

Monetary input remains a decimal string. Validation and comparison convert digits
to `bigint` minor units and never use JavaScript floating-point arithmetic.

Each transfer draft receives a cryptographically random idempotency key. A failed
or uncertain request retains that key. The console does not silently retry the
write. Transfer detail can replay the same body and key while router state is
available and explicitly explains `Idempotency-Replayed: true`.

Transfer detail polls only non-terminal transfers, pauses scheduled polling while
the tab is hidden, uses bounded query retries, keeps the last successful state
during temporary failures, and stops at `COMPLETED`, `REJECTED`, or `EXPIRED`.
The timeline renders only transitions returned by the history endpoint.

## Demo Lab

Admin can run four isolated, client-orchestrated scenarios:

1. successful `1000.00 + 100.00 - 125.50` settlement;
2. `RISK-REJECT` reservation, compensation, and rejection;
3. insufficient-funds reservation rejection;
4. exact body/key idempotency replay.

Stopping the runner aborts browser polling only. It does not cancel a backend
workflow and no destructive reset API exists.

## Environment and tests

Copy the frontend `.env.example` when overrides are needed. Never put a secret,
password, bearer token, database URL, Kafka credential, or Redis key in a `VITE_`
variable because Vite embeds it in browser assets.

The local Vite server maps the same-origin `/gateway-readiness` request to the
Gateway's public readiness probe. A static preview host must provide the same
narrow reverse-proxy mapping or set `VITE_GATEWAY_READINESS_URL` to an
equivalent CORS-enabled probe URL; it must not expose other Actuator endpoints.

```powershell
npm ci
npm run lint
npm run typecheck
npm run test
npm run build
npm run test:e2e
```

`test:e2e` is the fast route-fixture project. `test:e2e:real` runs only when
`LEDGERFLOW_REAL_E2E=true` and requires the complete local stack. Failure
screenshots, traces on retry, reports, coverage, built assets, and a frontend SBOM
are CI artifacts rather than uncontrolled committed screenshots.

Phase 5 deliberately has no public deployment, customer ownership model,
production identity configuration, real deposit, external payment network, real
message delivery, WebSocket feed, Kubernetes manifest, or destructive demo reset.
