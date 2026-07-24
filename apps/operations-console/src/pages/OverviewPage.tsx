import { useQueries, useQuery } from "@tanstack/react-query";
import { Link, useLocation } from "react-router-dom";
import { ArrowRight, Bell, CircleDollarSign, Landmark, Workflow } from "lucide-react";
import { listAccounts, listNotifications, listTransfers, type TransferStatus } from "@/api";
import { PageHeader } from "@/components/PageHeader";
import { LoadingState } from "@/components/States";
import { ProblemPanel } from "@/components/ProblemPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { formatDateTime, shortId } from "@/utils/format";

const workflowStatuses: TransferStatus[] = [
  "PENDING", "FUNDS_RESERVED", "RISK_APPROVED", "SETTLING",
  "COMPLETED", "COMPENSATING", "REJECTED", "EXPIRED",
];

export function OverviewPage() {
  const location = useLocation();
  const routeState = (location.state ?? {}) as { denied?: string };
  const [accounts, active, completed, rejected] = useQueries({
    queries: [
      { queryKey: ["accounts", "summary"], queryFn: () => listAccounts({ size: 1 }) },
      { queryKey: ["transfers", "active"], queryFn: async () => {
        const results = await Promise.all(
          ["PENDING", "FUNDS_RESERVED", "RISK_APPROVED", "SETTLING", "COMPENSATING"].map(
            (status) => listTransfers({ status: status as TransferStatus, size: 1 }),
          ),
        );
        return results.reduce((sum, result) => sum + result.data.totalElements, 0);
      }},
      { queryKey: ["transfers", "completed"], queryFn: () => listTransfers({ status: "COMPLETED", size: 1 }) },
      { queryKey: ["transfers", "rejected"], queryFn: () => listTransfers({ status: "REJECTED", size: 1 }) },
    ],
  });
  const recentTransfers = useQuery({
    queryKey: ["transfers", "recent"],
    queryFn: () => listTransfers({ size: 6 }),
  });
  const notifications = useQuery({
    queryKey: ["notifications", "recent"],
    queryFn: () => listNotifications({ size: 4 }),
  });
  const failure = [accounts, active, completed, rejected, recentTransfers, notifications]
    .find((query) => query.error)?.error;

  return (
    <>
      <PageHeader
        eyebrow="Live operations"
        title="See the workflow, not a simulation"
        description="Real account, transfer, and terminal-notification state read through the secured API Gateway."
        actions={<Link className="button button--primary" to="/transfers/new">Create transfer <ArrowRight aria-hidden="true" /></Link>}
      />
      {routeState.denied && (
        <div className="notice notice--warning" role="status">
          Your LedgerFlow role does not permit that write operation. Backend authorization remains authoritative.
        </div>
      )}
      {failure && <ProblemPanel error={failure} onRetry={() => void recentTransfers.refetch()} />}
      <section className="metric-grid" aria-label="Operational summary">
        <article className="metric-card"><Landmark aria-hidden="true" /><span>Visible accounts</span><strong>{accounts.data?.data.totalElements ?? "—"}</strong></article>
        <article className="metric-card"><Workflow aria-hidden="true" /><span>Active transfers</span><strong>{active.data ?? "—"}</strong></article>
        <article className="metric-card metric-card--healthy"><CircleDollarSign aria-hidden="true" /><span>Completed</span><strong>{completed.data?.data.totalElements ?? "—"}</strong></article>
        <article className="metric-card metric-card--danger"><Bell aria-hidden="true" /><span>Rejected</span><strong>{rejected.data?.data.totalElements ?? "—"}</strong></article>
      </section>
      <section className="split-grid">
        <article className="panel">
          <div className="section-heading"><div><span className="eyebrow">Newest first</span><h2>Recent transfer activity</h2></div><Link to="/transfers">All transfers</Link></div>
          {recentTransfers.isPending ? <LoadingState /> : (
            <div className="activity-list">
              {recentTransfers.data?.data.content.map((transfer) => (
                <Link to={`/transfers/${transfer.transferId}`} key={transfer.transferId}>
                  <StatusBadge status={transfer.status} />
                  <span><strong>{transfer.reference}</strong><small>{shortId(transfer.transferId)} · {formatDateTime(transfer.updatedAt)}</small></span>
                  <b>{transfer.currency} {transfer.amount}</b>
                </Link>
              ))}
              {recentTransfers.data?.data.content.length === 0 && <p>No transfers have been submitted.</p>}
            </div>
          )}
        </article>
        <article className="panel">
          <div className="section-heading"><div><span className="eyebrow">Persisted outcomes</span><h2>Recent notifications</h2></div><Link to="/notifications">Inspect records</Link></div>
          {notifications.isPending ? <LoadingState /> : (
            <div className="activity-list">
              {notifications.data?.data.content.map((item) => (
                <Link to={`/transfers/${item.transferId}`} key={item.notificationId}>
                  <StatusBadge status={item.finalTransferStatus} />
                  <span><strong>{item.type.replaceAll("_", " ")}</strong><small>{shortId(item.correlationId)}</small></span>
                </Link>
              ))}
              {notifications.data?.data.content.length === 0 && <p>No terminal notifications yet.</p>}
            </div>
          )}
        </article>
      </section>
      <section className="panel workflow-proof">
        <div><span className="eyebrow">Asynchronous by design</span><h2>One transfer, independently owned state</h2><p>The intake response is only the start. Kafka events move the workflow through account reservation, risk, settlement or compensation, and a durable notification record.</p></div>
        <ol className="pipeline">
          {["API Gateway", "Transfer intake", "Kafka", "Account reservation", "Risk decision", "Settlement / compensation", "Notification"].map((step) => <li key={step}>{step}</li>)}
        </ol>
        <div className="status-legend">
          {workflowStatuses.map((status) => <StatusBadge key={status} status={status} />)}
        </div>
      </section>
    </>
  );
}
