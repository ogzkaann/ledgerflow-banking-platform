import type { TransferStatus } from "@/api";

type Status = TransferStatus | "ACTIVE" | "FROZEN" | "CLOSED" | "HEALTHY" | "UNAVAILABLE";

const semanticClass: Record<Status, string> = {
  ACTIVE: "status--healthy",
  HEALTHY: "status--healthy",
  COMPLETED: "status--healthy",
  PENDING: "status--pending",
  FUNDS_RESERVED: "status--active",
  RISK_APPROVED: "status--active",
  SETTLING: "status--active",
  COMPENSATING: "status--warning",
  FROZEN: "status--warning",
  REJECTED: "status--danger",
  EXPIRED: "status--danger",
  CLOSED: "status--neutral",
  UNAVAILABLE: "status--danger",
};

export function StatusBadge({ status }: { readonly status: Status }) {
  return (
    <span className={`status-badge ${semanticClass[status]}`}>
      <span className="status-badge__dot" aria-hidden="true" />
      {status.replaceAll("_", " ")}
    </span>
  );
}
