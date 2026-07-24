import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { Plus, Search } from "lucide-react";
import { listTransfers, type TransferStatus } from "@/api";
import { usePermissions } from "@/auth/usePermissions";
import { CopyButton } from "@/components/CopyButton";
import { EmptyState, LoadingState } from "@/components/States";
import { PageHeader } from "@/components/PageHeader";
import { Pagination } from "@/components/Pagination";
import { ProblemPanel } from "@/components/ProblemPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { formatDateTime, shortId } from "@/utils/format";
import { formatMoney } from "@/utils/money";

const statuses: TransferStatus[] = ["PENDING", "FUNDS_RESERVED", "RISK_APPROVED", "SETTLING", "COMPLETED", "COMPENSATING", "REJECTED", "EXPIRED"];

export function TransfersPage() {
  const permissions = usePermissions();
  const [params, setParams] = useSearchParams();
  const page = Math.max(0, Number(params.get("page") ?? 0) || 0);
  const status = params.get("status") as TransferStatus | null;
  const reference = params.get("reference") ?? "";
  const sourceAccountId = params.get("source") ?? "";
  const destinationAccountId = params.get("destination") ?? "";
  const query = useQuery({
    queryKey: ["transfers", page, status, reference, sourceAccountId, destinationAccountId],
    queryFn: ({ signal }) => listTransfers({ page, size: 20, status: status ?? undefined, reference: reference || undefined, sourceAccountId: sourceAccountId || undefined, destinationAccountId: destinationAccountId || undefined }, signal),
  });
  const setFilter = (key: string, value: string) => setParams((current) => {
    if (value) current.set(key, value); else current.delete(key);
    current.delete("page");
    return current;
  });
  return (
    <>
      <PageHeader eyebrow="Transfer Service" title="Transfers" description="Newest transfer workflows and their current durable state."
        actions={permissions.canCreate ? <Link className="button button--primary" to="/transfers/new"><Plus aria-hidden="true" /> Create transfer</Link> : undefined} />
      <section className="filter-bar filter-bar--wide" aria-label="Transfer filters">
        <label>Status<select value={status ?? ""} onChange={(event) => setFilter("status", event.target.value)}><option value="">All statuses</option>{statuses.map((item) => <option key={item}>{item}</option>)}</select></label>
        <label>Reference<span className="input-with-icon"><Search aria-hidden="true" /><input value={reference} onChange={(event) => setFilter("reference", event.target.value)} placeholder="invoice" /></span></label>
        <label>Source account<input value={sourceAccountId} onChange={(event) => setFilter("source", event.target.value)} placeholder="Exact UUID" /></label>
        <label>Destination account<input value={destinationAccountId} onChange={(event) => setFilter("destination", event.target.value)} placeholder="Exact UUID" /></label>
      </section>
      {query.isPending ? <LoadingState label="Loading transfers" /> : query.isError ? <ProblemPanel error={query.error} onRetry={() => void query.refetch()} /> :
        query.data.data.content.length === 0 ? <EmptyState title="No matching transfers" detail="Change the filters or submit a new workflow." /> : (
          <section className="panel table-panel">
            <div className="table-scroll"><table><thead><tr><th>Status</th><th className="numeric">Amount</th><th>Source</th><th>Destination</th><th>Reference</th><th>Created</th><th>Updated</th><th>Correlation ID</th></tr></thead><tbody>
              {query.data.data.content.map((transfer) => <tr key={transfer.transferId}>
                <td><Link to={`/transfers/${transfer.transferId}`}><StatusBadge status={transfer.status} /></Link></td>
                <td className="numeric money">{formatMoney(transfer.amount, transfer.currency)}</td>
                <td><Link className="mono-link" to={`/accounts/${transfer.sourceAccountId}`}>{shortId(transfer.sourceAccountId)}</Link></td>
                <td><Link className="mono-link" to={`/accounts/${transfer.destinationAccountId}`}>{shortId(transfer.destinationAccountId)}</Link></td>
                <td><Link to={`/transfers/${transfer.transferId}`}>{transfer.reference}</Link></td><td>{formatDateTime(transfer.createdAt)}</td><td>{formatDateTime(transfer.updatedAt)}</td>
                <td><span className="id-cell"><code>{shortId(transfer.correlationId)}</code><CopyButton value={transfer.correlationId} label="Copy correlation ID" /></span></td>
              </tr>)}
            </tbody></table></div>
            <Pagination page={query.data.data.page} totalPages={query.data.data.totalPages} totalElements={query.data.data.totalElements} onPageChange={(next) => setParams((current) => { current.set("page", String(next)); return current; })} />
          </section>
        )}
    </>
  );
}
