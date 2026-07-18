# ADR 0001: Use a Modular Monorepo

- **Status:** Accepted
- **Date:** 2026-07-18

## Context

The system contains multiple independently deployable services, infrastructure definitions, contracts, tests, and a React console. A portfolio reviewer must be able to understand and run the complete system without discovering several repositories.

## Decision

Use one repository with separate Maven modules and independent service images. Each service owns its source, tests, database migrations, and configuration. Shared business-domain libraries are prohibited.

## Consequences

- Local development and review are simpler.
- Atomic contract and consumer changes are possible.
- CI must detect affected modules to avoid unnecessary work later.
- Service boundaries must be protected by architecture tests and data ownership rules.
