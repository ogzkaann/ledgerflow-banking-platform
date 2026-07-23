# Security Model

## Threats addressed

- unauthorized account or transfer access;
- replayed transfer requests;
- forged or malformed input;
- excessive request rates;
- secret leakage through source code or logs;
- insecure service-to-service defaults;
- dependency vulnerabilities.

Phase 4 implements defense-in-depth controls for the local operations API. These
controls demonstrate sound defaults, but the local single-node, plaintext
environment is not production banking infrastructure.

## Required controls

- OAuth 2.0 / OpenID Connect with RS256 JWT validation at the gateway and every
  resource service;
- issuer, lifetime, algorithm, `ledgerflow-api` audience, and realm-role validation;
- role checks inside business services, not only at the gateway;
- mandatory `Idempotency-Key` for transfer creation;
- Bean Validation plus domain validation;
- Redis-backed rate limiting with safe fallback behavior;
- secrets supplied through environment variables and `.env.example` placeholders;
- structured-log redaction for tokens, credentials, and sensitive fields;
- dependency and container scanning in CI;
- non-root container users and minimal runtime images;
- explicit CORS configuration for the operations console.

The implemented roles are operator, auditor, and admin. Liveness/readiness probes
are public; metrics and info require admin. Synthetic funding requires admin, while
auditors are read-only. Keycloak configuration, the complete authorization matrix,
and token instructions are documented in
[authentication and authorization](../security/authentication-authorization.md).

Customer ownership authorization, TLS, Kafka SASL/ACLs, workload identity or mTLS,
managed secrets, WAF/DDoS controls, and production identity hardening remain gaps
recorded in [SECURITY.md](../../SECURITY.md) and the
[threat model](../security/threat-model.md).

## Portfolio safety

The repository must contain no real bank credentials, personal records, production endpoints, or copied proprietary banking code. Seed data is synthetic.
