import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { listNotifications } from "@/api";
import { CopyButton } from "@/components/CopyButton";
import { EmptyState, LoadingState } from "@/components/States";
import { PageHeader } from "@/components/PageHeader";
import { Pagination } from "@/components/Pagination";
import { ProblemPanel } from "@/components/ProblemPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { formatDateTime, shortId } from "@/utils/format";

export function NotificationsPage() {
  const [params, setParams] = useSearchParams();
  const page = Math.max(0, Number(params.get("page") ?? 0) || 0);
  const transferId = params.get("transferId") ?? "";
  const type = params.get("type") as "TRANSFER_COMPLETED" | "TRANSFER_REJECTED" | null;
  const query = useQuery({
    queryKey: ["notifications", page, transferId, type],
    queryFn: ({ signal }) => listNotifications({ page, size: 20, transferId: transferId || undefined, type: type ?? undefined }, signal),
  });
  const setFilter = (key: string, value: string) => setParams((current) => {
    if (value) current.set(key, value); else current.delete(key);
    current.delete("page");
    return current;
  });
  return (
    <>
      <PageHeader eyebrow="Notification Service" title="Terminal notifications" description="Persisted demo records produced after terminal workflows. No real email, SMS, or push message is sent." />
      <div className="notice">These records prove durable consumer behavior; they are not delivery-provider receipts.</div>
      <section className="filter-bar">
        <label>Transfer ID<input value={transferId} onChange={(event) => setFilter("transferId", event.target.value)} placeholder="Exact UUID" /></label>
        <label>Type<select value={type ?? ""} onChange={(event) => setFilter("type", event.target.value)}><option value="">All types</option><option>TRANSFER_COMPLETED</option><option>TRANSFER_REJECTED</option></select></label>
      </section>
      {query.isPending ? <LoadingState label="Loading notifications" /> : query.isError ? <ProblemPanel error={query.error} onRetry={() => void query.refetch()} /> : query.data.data.content.length === 0 ? <EmptyState title="No terminal notifications" detail="A record appears after a transfer reaches COMPLETED or REJECTED." /> : (
        <section className="panel table-panel"><div className="table-scroll"><table><thead><tr><th>Type</th><th>Final status</th><th>Transfer ID</th><th>Template key</th><th>Correlation ID</th><th>Created</th></tr></thead><tbody>
          {query.data.data.content.map((item) => <tr key={item.notificationId}><td>{item.type.replaceAll("_", " ")}</td><td><StatusBadge status={item.finalTransferStatus} /></td><td><Link className="mono-link" to={`/transfers/${item.transferId}`}>{shortId(item.transferId)}</Link></td><td><code>{item.messageTemplateKey}</code></td><td><span className="id-cell"><code>{shortId(item.correlationId)}</code><CopyButton value={item.correlationId} label="Copy correlation ID" /></span></td><td>{formatDateTime(item.createdAt)}</td></tr>)}
        </tbody></table></div><Pagination page={query.data.data.page} totalPages={query.data.data.totalPages} totalElements={query.data.data.totalElements} onPageChange={(next) => setParams((current) => { current.set("page", String(next)); return current; })} /></section>
      )}
    </>
  );
}
