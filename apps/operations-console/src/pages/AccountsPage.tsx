import { useQuery } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router-dom";
import { Plus } from "lucide-react";
import { listAccounts } from "@/api";
import { usePermissions } from "@/auth/usePermissions";
import { CopyButton } from "@/components/CopyButton";
import { EmptyState, LoadingState } from "@/components/States";
import { PageHeader } from "@/components/PageHeader";
import { Pagination } from "@/components/Pagination";
import { ProblemPanel } from "@/components/ProblemPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { formatDateTime, shortId } from "@/utils/format";
import { formatMoney } from "@/utils/money";

export function AccountsPage() {
  const permissions = usePermissions();
  const [params, setParams] = useSearchParams();
  const page = Math.max(0, Number(params.get("page") ?? 0) || 0);
  const currency = params.get("currency") as "EUR" | "USD" | "GBP" | null;
  const status = params.get("status") as "ACTIVE" | "FROZEN" | "CLOSED" | null;
  const query = useQuery({
    queryKey: ["accounts", page, currency, status],
    queryFn: ({ signal }) => listAccounts({ page, size: 20, currency: currency ?? undefined, status: status ?? undefined }, signal),
  });
  const setFilter = (key: string, value: string) => {
    setParams((current) => {
      if (value) current.set(key, value); else current.delete(key);
      current.delete("page");
      return current;
    });
  };

  return (
    <>
      <PageHeader eyebrow="Account Service" title="Accounts" description="Balances and immutable ledger state owned by Account Service."
        actions={permissions.canCreate ? <Link className="button button--primary" to="/accounts/new"><Plus aria-hidden="true" /> Create account</Link> : undefined} />
      <section className="filter-bar" aria-label="Account filters">
        <label>Status<select value={status ?? ""} onChange={(event) => setFilter("status", event.target.value)}><option value="">All statuses</option><option>ACTIVE</option><option>FROZEN</option><option>CLOSED</option></select></label>
        <label>Currency<select value={currency ?? ""} onChange={(event) => setFilter("currency", event.target.value)}><option value="">All currencies</option><option>EUR</option><option>USD</option><option>GBP</option></select></label>
      </section>
      {query.isPending ? <LoadingState label="Loading accounts" /> : query.isError ? <ProblemPanel error={query.error} onRetry={() => void query.refetch()} /> :
        query.data.data.content.length === 0 ? <EmptyState title="No matching accounts" detail="Change the filters or create an account if your role allows it." /> : (
          <section className="panel table-panel">
            <div className="table-scroll"><table><thead><tr><th>Owner reference</th><th>Account ID</th><th>Currency</th><th className="numeric">Available</th><th className="numeric">Reserved</th><th>Status</th><th>Updated</th></tr></thead>
              <tbody>{query.data.data.content.map((account) => <tr key={account.accountId}>
                <td><Link to={`/accounts/${account.accountId}`}><strong>{account.ownerReference}</strong></Link></td>
                <td><span className="id-cell"><code title={account.accountId}>{shortId(account.accountId)}</code><CopyButton value={account.accountId} label="Copy account ID" /></span></td>
                <td>{account.currency}</td><td className="numeric money">{formatMoney(account.availableBalance, account.currency)}</td><td className="numeric money">{formatMoney(account.reservedBalance, account.currency)}</td>
                <td><StatusBadge status={account.status} /></td><td><time dateTime={account.updatedAt}>{formatDateTime(account.updatedAt)}</time></td>
              </tr>)}</tbody></table></div>
            <Pagination page={query.data.data.page} totalPages={query.data.data.totalPages} totalElements={query.data.data.totalElements}
              onPageChange={(next) => setParams((current) => { current.set("page", String(next)); return current; })} />
          </section>
        )}
    </>
  );
}
