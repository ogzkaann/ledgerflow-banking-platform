# ADR 0005: Use Structured Logs and Elastic Stack

- **Status:** Accepted
- **Date:** 2026-07-18

## Decision

Services emit JSON logs to standard output. Local infrastructure collects, transforms, indexes, and displays logs through Logstash, Elasticsearch, and Kibana using matching stack versions.

## Consequences

- Logs are searchable by transfer, account, correlation, event, and service identifiers.
- Sensitive values require centralized redaction rules.
- The local stack is resource-intensive and will use an optional Docker Compose profile.
