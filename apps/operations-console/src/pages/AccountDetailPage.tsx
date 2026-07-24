import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { fundAccount, getAccount, getLedger } from "@/api";
import { usePermissions } from "@/auth/usePermissions";
import { appConfig } from "@/config";
import { CopyButton } from "@/components/CopyButton";
import { FieldValue } from "@/components/FieldValue";
import { LoadingState, EmptyState } from "@/components/States";
import { PageHeader } from "@/components/PageHeader";
import { Pagination } from "@/components/Pagination";
import { ProblemPanel } from "@/components/ProblemPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { formatDateTime, shortId } from "@/utils/format";
import { formatMoney, normalizeMoney } from "@/utils/money";

export function AccountDetailPage() {
  const { accountId = "" } = useParams();
  const location = useLocation();
  const permissions = usePermissions();
  const client = useQueryClient();
  const [ledgerPage, setLedgerPage] = useState(0);
  const [amount, setAmount] = useState("");
  const [reference, setReference] = useState("");
  const account = useQuery({ queryKey: ["account", accountId], queryFn: ({ signal }) => getAccount(accountId, signal), enabled: Boolean(accountId) });
  const ledger = useQuery({ queryKey: ["ledger", accountId, ledgerPage], queryFn: ({ signal }) => getLedger(accountId, ledgerPage, 20, signal), enabled: Boolean(accountId) });
  const funding = useMutation({
    mutationFn: () => {
      const normalized = normalizeMoney(amount);
      if (!normalized || normalized === "0.00") throw new Error("A positive funding amount is required.");
      return fundAccount(accountId, { amount: normalized, reference: reference.trim() });
    },
    onSuccess: async () => {
      setAmount(""); setReference("");
      await Promise.all([client.invalidateQueries({ queryKey: ["account", accountId] }), client.invalidateQueries({ queryKey: ["ledger", accountId] })]);
    },
  });
  if (account.isPending) return <LoadingState label="Loading account detail" />;
  if (account.isError) return <ProblemPanel error={account.error} onRetry={() => void account.refetch()} />;
  const item = account.data.data;
  const canFund = permissions.canFundDemo && appConfig.enableDemoFunding;
  const submitFunding = (event: FormEvent) => {
    event.preventDefault();
    if (normalizeMoney(amount) && normalizeMoney(amount) !== "0.00" && reference.trim()) funding.mutate();
  };
  const routeState = (location.state ?? {}) as { created?: boolean; correlationId?: string };
  return (
    <>
      <Link className="back-link" to="/accounts"><ArrowLeft aria-hidden="true" /> Accounts</Link>
      <PageHeader eyebrow="Account detail" title={item.ownerReference} description="Current balance projection with its immutable supporting ledger." />
      {routeState.created && <div className="notice notice--success">Account created successfully.{routeState.correlationId && <> Correlation: <code>{routeState.correlationId}</code></>}</div>}
      <section className="balance-grid">
        <article className="balance-card balance-card--primary"><span>Available balance</span><strong>{formatMoney(item.availableBalance, item.currency)}</strong></article>
        <article className="balance-card"><span>Reserved balance</span><strong>{formatMoney(item.reservedBalance, item.currency)}</strong></article>
      </section>
      <section className="panel"><dl className="detail-grid"><FieldValue label="Status"><StatusBadge status={item.status} /></FieldValue><FieldValue label="Currency">{item.currency}</FieldValue><FieldValue label="Version">{item.version}</FieldValue><FieldValue label="Account ID" mono>{item.accountId} <CopyButton value={item.accountId} /></FieldValue><FieldValue label="Created">{formatDateTime(item.createdAt)}</FieldValue><FieldValue label="Updated">{formatDateTime(item.updatedAt)}</FieldValue></dl></section>
      {canFund && <section className="panel demo-funding"><div><span className="eyebrow">Admin · local/test only</span><h2>Demo funding</h2><p>This creates synthetic credit ledger state. It is not a deposit or banking integration.</p></div>
        <form onSubmit={submitFunding}><label>Amount<input inputMode="decimal" placeholder="1000.00" value={amount} onChange={(event) => setAmount(event.target.value)} /></label><label>Unique reference<input value={reference} onChange={(event) => setReference(event.target.value)} /></label><button className="button button--primary" disabled={!normalizeMoney(amount) || normalizeMoney(amount) === "0.00" || !reference.trim() || funding.isPending} type="submit">Add demo funding</button></form>
        {funding.error && <ProblemPanel error={funding.error} />}{funding.isSuccess && <div className="notice notice--success">Synthetic funding persisted with an immutable credit entry.</div>}
      </section>}
      <section className="panel table-panel"><div className="section-heading"><div><span className="eyebrow">Append-only evidence</span><h2>Immutable ledger</h2></div></div>
        {ledger.isPending ? <LoadingState /> : ledger.isError ? <ProblemPanel error={ledger.error} onRetry={() => void ledger.refetch()} /> : ledger.data.data.content.length === 0 ? <EmptyState title="No ledger entries" detail="This account has not received a credit or debit." /> :
          <><div className="table-scroll"><table><thead><tr><th>Timestamp</th><th>Type</th><th className="numeric">Amount</th><th>Reference</th><th>Entry ID</th></tr></thead><tbody>{ledger.data.data.content.map((entry) => <tr key={entry.ledgerEntryId}><td>{formatDateTime(entry.createdAt)}</td><td><span className={`entry-type entry-type--${entry.type.toLowerCase()}`}>{entry.type}</span></td><td className="numeric money">{entry.type === "DEBIT" ? "−" : "+"}{formatMoney(entry.amount, entry.currency)}</td><td>{entry.reference}</td><td><span className="id-cell"><code>{shortId(entry.ledgerEntryId)}</code><CopyButton value={entry.ledgerEntryId} /></span></td></tr>)}</tbody></table></div><Pagination page={ledger.data.data.page} totalPages={ledger.data.data.totalPages} totalElements={ledger.data.data.totalElements} onPageChange={setLedgerPage} /></>}
      </section>
    </>
  );
}
