# Contributing

## Commit style

Use Conventional Commits with focused changes:

- `feat(account): add ledger posting use case`
- `fix(transfer): prevent duplicate settlement`
- `test(risk): cover velocity rejection rule`
- `docs(architecture): explain outbox delivery semantics`
- `build: configure Maven modules`

## Pull request expectations

Every pull request must explain:

- what changed and why;
- architectural or contract impact;
- validation performed;
- failure scenarios considered;
- follow-up work that is intentionally out of scope.

Run `./mvnw clean verify` (or `.\mvnw.cmd clean verify` on Windows) before opening a pull request.

## Engineering rules

- Do not share service database tables.
- Do not use floating-point values for money.
- Do not add unbounded retries.
- Do not publish unversioned events.
- Do not introduce a dependency without documenting its purpose.
- Keep domain logic independent of Spring where practical.
