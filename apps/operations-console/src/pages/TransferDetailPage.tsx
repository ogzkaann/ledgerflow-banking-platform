import { useMutation, useQuery } from "@tanstack/react-query";
import { Link, useLocation, useParams } from "react-router-dom";
import { ArrowLeft, ExternalLink } from "lucide-react";
import { createTransfer, getTransfer, getTransferHistory, listNotifications, type CreateTransferBody } from "@/api";
import { appConfig } from "@/config";
import { CopyButton } from "@/components/CopyButton";
import { FieldValue } from "@/components/FieldValue";
import { LoadingState } from "@/components/States";
import { PageHeader } from "@/components/PageHeader";
import { ProblemPanel } from "@/components/ProblemPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { isTerminalStatus, TransferTimeline } from "@/components/TransferTimeline";
import { formatDateTime } from "@/utils/format";
import { formatMoney } from "@/utils/money";

interface TransferRouteState {
  readonly accepted?: boolean;
  readonly replayed?: boolean;
  readonly idempotencyKey?: string;
  readonly replayBody?: CreateTransferBody;
  readonly correlationId?: string | null;
}

export function TransferDetailPage() {
  const { transferId = "" } = useParams();
  const location = useLocation();
  const state = (location.state ?? {}) as TransferRouteState;
  const transfer = useQuery({
    queryKey: ["transfer", transferId],
    queryFn: ({ signal }) => getTransfer(transferId, signal),
    enabled: Boolean(transferId),
    retry: 2,
    refetchInterval: (query) => {
      const result = query.state.data;
      return document.visibilityState === "visible" && result && !isTerminalStatus(result.data.status) ? 1_500 : false;
    },
  });
  const currentStatus = transfer.data?.data.status;
  const history = useQuery({
    queryKey: ["transfer-history", transferId],
    queryFn: ({ signal }) => getTransferHistory(transferId, signal),
    enabled: Boolean(transferId),
    refetchInterval: currentStatus && !isTerminalStatus(currentStatus) && document.visibilityState === "visible" ? 1_800 : false,
  });
  const notifications = useQuery({
    queryKey: ["notifications", transferId],
    queryFn: ({ signal }) => listNotifications({ transferId, size: 20 }, signal),
    enabled: Boolean(transferId) && Boolean(currentStatus && isTerminalStatus(currentStatus)),
  });
  const replay = useMutation({
    mutationFn: () => {
      if (!state.replayBody || !state.idempotencyKey) throw new Error("Replay evidence is not available after a fresh navigation.");
      return createTransfer(state.replayBody, state.idempotencyKey, state.correlationId ?? undefined);
    },
  });
  if (transfer.isPending) return <LoadingState label="Loading transfer workflow" />;
  if (transfer.isError) return <ProblemPanel error={transfer.error} onRetry={() => void transfer.refetch()} />;
  const item = transfer.data.data;
  const kibanaQuery = encodeURIComponent(`correlation.id:"${item.correlationId}"`);
  return (
    <>
      <Link className="back-link" to="/transfers"><ArrowLeft aria-hidden="true" /> Transfers</Link>
      <PageHeader eyebrow="Transfer workflow" title={item.reference} description="Persisted state from Transfer Service, refreshed only while the workflow is active." actions={<StatusBadge status={item.status} />} />
      {(state.accepted || replay.isSuccess) && <div className={`notice ${state.replayed || replay.data?.replayed ? "notice--replay" : "notice--success"}`}>
        {state.replayed || replay.data?.replayed ? <><strong>This request matched an earlier submission.</strong> No duplicate transfer was created.</> : <><strong>202 Accepted.</strong> The asynchronous workflow is now processing.</>}
      </div>}
      {transfer.isFetching && !isTerminalStatus(item.status) && <div className="poll-indicator" role="status" aria-live="polite">Following live state · {item.status.replaceAll("_", " ")}</div>}
      <section className="detail-layout">
        <article className="panel">
          <div className="amount-hero"><span>Transfer amount</span><strong>{formatMoney(item.amount, item.currency)}</strong></div>
          <dl className="detail-grid"><FieldValue label="Transfer ID" mono>{item.transferId} <CopyButton value={item.transferId} /></FieldValue><FieldValue label="Version">{item.version}</FieldValue><FieldValue label="Source"><Link to={`/accounts/${item.sourceAccountId}`}>{item.sourceAccountId}</Link></FieldValue><FieldValue label="Destination"><Link to={`/accounts/${item.destinationAccountId}`}>{item.destinationAccountId}</Link></FieldValue><FieldValue label="Created">{formatDateTime(item.createdAt)}</FieldValue><FieldValue label="Updated">{formatDateTime(item.updatedAt)}</FieldValue><FieldValue label="Correlation ID" mono>{item.correlationId} <CopyButton value={item.correlationId} label="Copy correlation ID" /></FieldValue></dl>
          <div className="external-links"><a href={`${appConfig.links.kibana}/app/discover#/?_a=(query:(language:kuery,query:'${kibanaQuery}'))`} target="_blank" rel="noreferrer">Search correlation in Kibana <ExternalLink aria-hidden="true" /></a><a href={appConfig.links.grafana} target="_blank" rel="noreferrer">Open Grafana <ExternalLink aria-hidden="true" /></a></div>
          {state.idempotencyKey && state.replayBody && <div className="replay-box"><h2>Idempotency proof</h2><p>Replay this exact body and key. The backend must return the same transfer without another money movement.</p><div className="technical-inline"><code>{state.idempotencyKey}</code><CopyButton value={state.idempotencyKey} /></div><button className="button button--secondary" disabled={replay.isPending} onClick={() => replay.mutate()} type="button">{replay.isPending ? "Replaying…" : "Replay same request"}</button>{replay.error && <ProblemPanel error={replay.error} />}</div>}
        </article>
        <article className="panel"><div className="section-heading"><div><span className="eyebrow">Actual transitions only</span><h2>Immutable state history</h2></div></div>{history.isPending ? <LoadingState /> : history.isError ? <ProblemPanel error={history.error} onRetry={() => void history.refetch()} /> : <TransferTimeline history={history.data.data} currentStatus={item.status} />}</article>
      </section>
      {isTerminalStatus(item.status) && <section className="panel"><div className="section-heading"><div><span className="eyebrow">Terminal record</span><h2>Notification result</h2></div></div>{notifications.isPending ? <LoadingState /> : notifications.error ? <ProblemPanel error={notifications.error} /> : notifications.data && notifications.data.data.content.length ? notifications.data.data.content.map((notification) => <div className="notification-summary" key={notification.notificationId}><StatusBadge status={notification.finalTransferStatus} /><div><strong>{notification.type.replaceAll("_", " ")}</strong><p>Template: <code>{notification.messageTemplateKey}</code> · {formatDateTime(notification.createdAt)}</p></div></div>) : <p>No notification is persisted yet. The notification consumer may still be catching up.</p>}</section>}
    </>
  );
}
