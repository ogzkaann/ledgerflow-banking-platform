import { useMutation, useQuery } from "@tanstack/react-query";
import { useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createTransfer, listAccounts, type CreateTransferBody } from "@/api";
import { CopyButton } from "@/components/CopyButton";
import { PageHeader } from "@/components/PageHeader";
import { ProblemPanel } from "@/components/ProblemPanel";
import { LoadingState } from "@/components/States";
import { createIdempotencyKey } from "@/utils/idempotency";
import { formatMoney, normalizeMoney } from "@/utils/money";

export function CreateTransferPage() {
  const navigate = useNavigate();
  const [idempotencyKey] = useState(() => createIdempotencyKey("transfer"));
  const [sourceId, setSourceId] = useState("");
  const [destinationId, setDestinationId] = useState("");
  const [amount, setAmount] = useState("");
  const [reference, setReference] = useState("");
  const accounts = useQuery({ queryKey: ["accounts", "selectable"], queryFn: () => listAccounts({ size: 100, status: "ACTIVE" }) });
  const source = useMemo(() => accounts.data?.data.content.find((item) => item.accountId === sourceId), [accounts.data, sourceId]);
  const destination = useMemo(() => accounts.data?.data.content.find((item) => item.accountId === destinationId), [accounts.data, destinationId]);
  const normalizedAmount = normalizeMoney(amount);
  const errors = [
    sourceId === destinationId && sourceId ? "Source and destination must differ." : null,
    source && destination && source.currency !== destination.currency ? "Accounts must use the same currency." : null,
    amount && (!normalizedAmount || normalizedAmount === "0.00") ? "Enter a positive amount with at most two decimal places." : null,
    reference.trim().length > 140 ? "Reference must be 140 characters or fewer." : null,
  ].filter(Boolean) as string[];
  const mutation = useMutation({
    mutationFn: (body: CreateTransferBody) => createTransfer(body, idempotencyKey),
    onSuccess: (result, body) => navigate(`/transfers/${result.data.transferId}`, {
      state: { accepted: true, replayed: result.replayed, idempotencyKey, replayBody: body, correlationId: result.correlationId },
    }),
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!source || !destination || !normalizedAmount || normalizedAmount === "0.00" || !reference.trim() || errors.length) return;
    mutation.mutate({ sourceAccountId: source.accountId, destinationAccountId: destination.accountId, amount: normalizedAmount, currency: source.currency, reference: reference.trim() });
  };
  if (accounts.isPending) return <LoadingState label="Loading selectable accounts" />;
  return (
    <>
      <PageHeader eyebrow="Operator action" title="Create transfer" description="Submit a real asynchronous workflow. Account reservation and funds availability remain backend decisions." />
      {accounts.error && <ProblemPanel error={accounts.error} onRetry={() => void accounts.refetch()} />}
      <section className="panel form-panel form-panel--wide">
        <form onSubmit={submit} noValidate>
          <div className="form-grid">
            <label>Source account<select value={sourceId} onChange={(event) => setSourceId(event.target.value)}><option value="">Select source</option>{accounts.data?.data.content.map((account) => <option key={account.accountId} value={account.accountId}>{account.ownerReference} · {formatMoney(account.availableBalance, account.currency)}</option>)}</select></label>
            <label>Destination account<select value={destinationId} onChange={(event) => setDestinationId(event.target.value)}><option value="">Select destination</option>{accounts.data?.data.content.filter((account) => account.accountId !== sourceId).map((account) => <option key={account.accountId} value={account.accountId}>{account.ownerReference} · {account.currency}</option>)}</select></label>
            {source && <div className="account-preview"><span>Source available</span><strong>{formatMoney(source.availableBalance, source.currency)}</strong><small>Reserved {formatMoney(source.reservedBalance, source.currency)}</small></div>}
            {destination && <div className="account-preview"><span>Destination available</span><strong>{formatMoney(destination.availableBalance, destination.currency)}</strong><small>Reserved {formatMoney(destination.reservedBalance, destination.currency)}</small></div>}
          </div>
          <label>Amount<input inputMode="decimal" placeholder="125.50" value={amount} onChange={(event) => setAmount(event.target.value)} /></label>
          <label>Reference<input maxLength={140} placeholder="invoice-2026-001" value={reference} onChange={(event) => setReference(event.target.value)} /></label>
          <details className="technical-detail"><summary>Advanced request protection</summary><p>This key is retained after a failed or uncertain submission. It is never regenerated automatically.</p><div className="technical-inline"><span>Idempotency key</span><code>{idempotencyKey}</code><CopyButton value={idempotencyKey} label="Copy idempotency key" /></div></details>
          {errors.map((error) => <p className="field-error" key={error}>{error}</p>)}
          {mutation.error && <><ProblemPanel error={mutation.error} /><div className="notice notice--warning">An uncertain write is not retried automatically. Submit again only when you intend to safely replay this exact request with the preserved key above.</div></>}
          <div className="form-actions"><Link className="button button--quiet" to="/transfers">Cancel</Link><button className="button button--primary" disabled={!source || !destination || !normalizedAmount || normalizedAmount === "0.00" || !reference.trim() || errors.length > 0 || mutation.isPending} type="submit">{mutation.isPending ? "Submitting…" : "Submit transfer"}</button></div>
        </form>
      </section>
    </>
  );
}
