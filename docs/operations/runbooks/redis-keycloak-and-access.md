# Redis, Keycloak, and access runbooks

## Redis unavailable

Symptoms: gateway writes fail closed during rate-limit evaluation; Transfer
Service logs cache degradation, while PostgreSQL remains authoritative.

1. Check `docker compose ps redis` and `docker compose logs --tail 100 redis`.
2. Confirm Transfer Service readiness and PostgreSQL durability independently.
3. Restart Redis: `docker compose restart redis`.
4. Verify gateway rate limiting resumes and a repeated transfer request is
   repaired from durable idempotency.

Do not copy balances or transfers into Redis. Escalate sustained outage or
unexpected money-state dependency.

## Keycloak unavailable

Symptoms: token acquisition fails and uncached JWK/metadata resolution may reject
authentication. Existing services must fail closed.

1. Check `docker compose ps keycloak` and its health/logs.
2. Verify the realm file parses and required client secret environment variables
   are present without printing their values.
3. Restart Keycloak: `docker compose restart keycloak`.
4. Confirm discovery, JWK, and client-credentials token endpoints.
5. Obtain a new test token and verify issuer/audience/role before API access.

Do not disable JWT validation or add a bypass token. Escalate realm import errors,
signing-key loss, or suspected credential compromise.

## Authentication failures

Symptoms: `AuthenticationFailureSpike`, repeated `401`, or Keycloak login/client
errors.

1. Correlate gateway security metrics with Keycloak logs.
2. Decode a synthetic token locally without logging it; verify `iss`, `aud`,
   `exp`, `nbf`, `alg`, and `azp`.
3. Check time synchronization and configured issuer/JWK URLs.
4. Rotate a compromised client secret and restart only affected scrapers/tools.
5. Verify a valid token succeeds and wrong audience/expired tokens still fail.

Never paste bearer tokens into tickets or Kibana. Escalate suspected forgery,
stolen credentials, or signing-key compromise.

## Authorization failures

Symptoms: authenticated requests return `403` or
`security.authorization.denials` rises.

1. Identify endpoint/method and required role from the authorization matrix.
2. Inspect only the bounded `realm_access.roles` claim from a synthetic token.
3. Verify gateway and downstream service agree.
4. Correct Keycloak role assignment; do not broaden application matchers as a
   quick fix.
5. Re-test allowed and denied roles, direct service access, funding, and metrics.

Escalate unexplained admin authority, privilege escalation, or policy drift.
