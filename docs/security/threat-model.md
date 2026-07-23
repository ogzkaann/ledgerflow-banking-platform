# Threat model

This concise STRIDE-style review covers the Phase 4 local/demo boundary. It is
an engineering inventory, not a certification.

| Asset | Threat | Current control | Residual risk / production control |
| --- | --- | --- | --- |
| Operations API | Stolen or forged JWT | RS256, issuer, lifetime, audience, and role validation at gateway and services | Local HTTP exposes bearer tokens; require TLS, hardened IdP, short lifetimes, revocation strategy |
| Authorization | Wrong audience or privilege escalation | Required `ledgerflow-api` audience and explicit endpoint role matrix | Add customer ownership and policy decision auditing before customer use |
| Downstream services | Gateway bypass | Every public service validates JWT independently | Add private networks, workload identity, mTLS, and firewall policy |
| Identity | Spoofed identity headers | Gateway strips untrusted identity headers; services trust only validated JWT | Enforce equivalent edge proxy stripping in production |
| API capacity | Brute force and abuse | Subject/client Redis rate limits, bounded IP fallback, Keycloak brute-force protection | Add WAF, adaptive limits, distributed abuse detection |
| Transfer intake | Idempotency-key abuse and request replay | Strict bounded key format, durable fingerprint, rate limits | Scope keys to authenticated business principals and expire by policy |
| Rate limiting | Redis outage | Protected writes fail closed; Redis is not financial state | Use highly available Redis and explicit degraded-read policy |
| Money movement | Kafka duplication or replay | Processed-event tables, unique IDs, guarded transitions, immutable ledger | Add authenticated Kafka principals and replay monitoring |
| Event boundary | Malicious/malformed Kafka payload | Explicit JSON DTOs, version validation, bounded retry, DLT | Add schema registry enforcement and producer ACLs |
| DLT | DLT poisoning or unsafe replay | DLT is diagnostic; runbook forbids blind replay | Build reviewed quarantine/replay tooling and immutable approvals |
| Logs | Log injection and secret leakage | Structured JSON, bounded correlation format, no body/header logging | Central redaction tests and restricted immutable log archive |
| Secrets | Database/Redis/IdP credential exposure | Environment injection, ignored `.env`, secret scan | Managed secret store, rotation, workload identity |
| Browser boundary | Cross-origin abuse | Exact origin list, explicit methods/headers, no credentials | Phase 5 CSP and OIDC browser hardening |
| Operations | Actuator exposure | Only probes public; metrics/info admin; dangerous endpoints unexposed | Separate management network and Prometheus workload identity |
| Availability | High-cardinality metric denial of service | No account, transfer, correlation, or idempotency tags | Enforce registry filters and telemetry budgets |
| Local demo | Unauthorized synthetic funding or data tampering | Funding exists only in local/test and requires admin | Remove synthetic funding from deployable production artifact |
| Audit trail | Database credential compromise | Separate service credentials and immutable application rows | External append-only audit storage and database activity monitoring |

## Explicit production gaps

Local Kafka is plaintext and has no SASL, TLS, or ACLs. Keycloak uses
development mode. Example databases, Redis, Elasticsearch, and dashboards use
local-only passwords. There is no WAF, managed secret storage, customer
ownership authorization, workload identity, regulatory control set, immutable
external audit archive, production incident response organization, or public
deployment. These gaps must not be described as solved by Phase 4.
