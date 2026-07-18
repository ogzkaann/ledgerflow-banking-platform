# Event Model

## Event envelope

Every Kafka message uses a stable envelope:

```json
{
  "eventId": "01J...",
  "eventType": "ledgerflow.transfer.initiated.v1",
  "eventVersion": 1,
  "occurredAt": "2026-07-18T12:00:00Z",
  "correlationId": "01J...",
  "causationId": "01J...",
  "producer": "transfer-service",
  "payload": {}
}
```

## Initial topics

| Topic | Producer | Consumers | Key |
| --- | --- | --- | --- |
| `ledgerflow.transfer.commands.v1` | Transfer Service | Account Service | transfer ID |
| `ledgerflow.account.events.v1` | Account Service | Transfer, Risk | transfer ID |
| `ledgerflow.risk.events.v1` | Risk Service | Transfer, Account | transfer ID |
| `ledgerflow.transfer.events.v1` | Transfer Service | Notification | transfer ID |
| `ledgerflow.*.dlt.v1` | Retry recoverer | Operations | original key |

## Compatibility rules

- Existing fields are never redefined.
- New optional fields may be added within a version.
- Breaking payload changes require a new event type version.
- Consumers ignore unknown optional fields.
- Contract examples and schemas are validated in CI.

## Ordering

Transfer ID is the Kafka message key for workflow events. This preserves ordering for a single transfer while allowing independent transfers to process in parallel.

## Delivery semantics

Kafka delivery is treated as at-least-once. Exactly-once business behavior is achieved through idempotent consumers, unique database constraints, and guarded state transitions rather than relying on broker marketing terminology.
