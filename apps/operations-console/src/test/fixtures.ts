import type { Account, Notification, Transfer, TransferHistory } from "@/api";

export const accountFixture: Account = {
  accountId: "11111111-1111-4111-8111-111111111111",
  ownerReference: "portfolio-source",
  currency: "EUR",
  status: "ACTIVE",
  availableBalance: "1000.00",
  reservedBalance: "0.00",
  version: 2,
  createdAt: "2026-07-24T12:00:00Z",
  updatedAt: "2026-07-24T12:05:00Z",
};

export const transferFixture: Transfer = {
  transferId: "33333333-3333-4333-8333-333333333333",
  sourceAccountId: accountFixture.accountId,
  destinationAccountId: "22222222-2222-4222-8222-222222222222",
  amount: "125.50",
  currency: "EUR",
  reference: "invoice-portfolio",
  status: "COMPLETED",
  correlationId: "corr-portfolio-001",
  createdAt: "2026-07-24T12:10:00Z",
  updatedAt: "2026-07-24T12:10:05Z",
  version: 4,
};

export const historyFixture: TransferHistory[] = [
  { transitionId: "40000000-0000-4000-8000-000000000001", fromStatus: null, toStatus: "PENDING", reason: "transfer-created", occurredAt: "2026-07-24T12:10:00Z", sequence: 1 },
  { transitionId: "40000000-0000-4000-8000-000000000002", fromStatus: "PENDING", toStatus: "FUNDS_RESERVED", reason: "funds-reserved", occurredAt: "2026-07-24T12:10:01Z", sequence: 2 },
  { transitionId: "40000000-0000-4000-8000-000000000003", fromStatus: "FUNDS_RESERVED", toStatus: "RISK_APPROVED", reason: "risk-approved", occurredAt: "2026-07-24T12:10:02Z", sequence: 3 },
  { transitionId: "40000000-0000-4000-8000-000000000004", fromStatus: "RISK_APPROVED", toStatus: "SETTLING", reason: "settlement-requested", occurredAt: "2026-07-24T12:10:03Z", sequence: 4 },
  { transitionId: "40000000-0000-4000-8000-000000000005", fromStatus: "SETTLING", toStatus: "COMPLETED", reason: "settlement-completed", occurredAt: "2026-07-24T12:10:05Z", sequence: 5 },
];

export const notificationFixture: Notification = {
  notificationId: "50000000-0000-4000-8000-000000000001",
  transferId: transferFixture.transferId,
  eventId: "50000000-0000-4000-8000-000000000002",
  type: "TRANSFER_COMPLETED",
  finalTransferStatus: "COMPLETED",
  correlationId: transferFixture.correlationId,
  messageTemplateKey: "transfer.completed",
  createdAt: "2026-07-24T12:10:06Z",
};
