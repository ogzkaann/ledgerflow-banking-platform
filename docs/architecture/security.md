# Security Model

## Threats addressed

- unauthorized account or transfer access;
- replayed transfer requests;
- forged or malformed input;
- excessive request rates;
- secret leakage through source code or logs;
- insecure service-to-service defaults;
- dependency vulnerabilities.

## Controls

- OAuth 2.0 / OpenID Connect with JWT validation at the gateway and resource services;
- ownership checks inside business services, not only at the gateway;
- mandatory `Idempotency-Key` for transfer creation;
- Bean Validation plus domain validation;
- Redis-backed rate limiting with safe fallback behavior;
- secrets supplied through environment variables and `.env.example` placeholders;
- structured-log redaction for tokens, credentials, and sensitive fields;
- dependency and container scanning in CI;
- non-root container users and minimal runtime images;
- explicit CORS configuration for the operations console.

## Portfolio safety

The repository must contain no real bank credentials, personal records, production endpoints, or copied proprietary banking code. Seed data is synthetic.
