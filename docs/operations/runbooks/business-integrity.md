# Business integrity runbooks

## Elevated transfer rejection rate

Symptoms: `transfer_rejected_total` rises, completion ratio drops, or operators
report normal transfers rejected.

1. Break down expected account rejection reasons and risk outcomes in logs.
2. Verify risk threshold, blocked marker, and rule version.
3. Check Account/Risk/PostgreSQL/Kafka health and outbox lag.
4. Sample synthetic rejected transfers and confirm compensation restored funds.
5. Correct configuration only through reviewed environment changes.

Do not retry a genuine business rejection or bypass risk. Escalate a sudden
unexplained change, incorrect compensation, or inconsistent rule versions.

## Ledger reconciliation failure

Symptoms: reconciliation test/query fails, balance and immutable ledger disagree,
or duplicate transfer references appear.

1. Stop initiating new demo transfers while preserving all evidence.
2. Record account IDs, transfer ID, reservation, ledger references, transfer
   history, processed events, and outbox events using read-only queries.
3. Do not edit balances, ledger rows, reservations, history, or deduplication
   records.
4. Compare the expected reserve/settle/release equations and database constraints.
5. Escalate immediately for code/data review and recovery design.

There is no safe casual retry. Verification requires restored reconciliation,
unique debit/credit references, correct reservation state, and full workflow
history.

## Notification lag

Symptoms: transfer is terminal but no notification exists, notification consumer
processing stops, or Kafka lag grows.

1. Verify `transfer.completed`/`transfer.rejected` outbox status is `PUBLISHED`.
2. Check Notification readiness, PostgreSQL, Kafka listener failures, and DLT.
3. Search by transfer, event, and correlation IDs.
4. Restore dependencies and allow the idempotent consumer to resume.
5. Verify exactly one notification and one processed-event row.

Do not insert notifications manually or delete processed events. Escalate if the
terminal event cannot be located or duplicates appear.
