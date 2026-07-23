# Security policy

## Supported version

Security fixes are applied to the current `main` branch of this portfolio
repository. There is no supported production release or compatibility promise.

## Reporting a vulnerability

Do not open a public issue containing credentials, tokens, exploit payloads, or
private data. Use GitHub's private vulnerability-reporting feature for this
repository when available. Otherwise contact the repository owner through the
private contact method shown on the owner's GitHub profile.

Include the affected commit, reproducible steps, impact, and a safe proof of
concept. Allow reasonable time for confirmation and correction before public
disclosure.

## Portfolio limitations

LedgerFlow uses synthetic data and must never process real money, real customer
records, or production credentials. The local environment intentionally uses
plaintext HTTP and Kafka, local-only example passwords, a development Keycloak,
and single-node infrastructure. It is not a certified banking system.

Production gaps include managed secrets, hardened identity infrastructure,
TLS/SASL and Kafka ACLs, workload identity or mTLS, customer ownership
authorization, a WAF, immutable external audit storage, regulatory controls,
multi-zone infrastructure, and a formally managed vulnerability process.

## Secret handling

- Real secrets belong in environment variables or an external secret manager.
- `.env`, generated tokens, logs, database files, and observability data are
  ignored and must not be committed.
- `.env.example` contains identifiable local-only demonstration values.
- Access tokens, authorization codes, client secrets, passwords, and complete
  request bodies must never be logged.
- Rotate a secret immediately if it is accidentally disclosed, then remove it
  from Git history using an approved coordinated process.
