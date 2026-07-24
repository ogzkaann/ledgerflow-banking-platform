import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2, FlaskConical, Play, Square } from "lucide-react";
import {
  createAccount, createTransfer, fundAccount, getAccount, getLedger, getTransfer,
  listNotifications, type Account, type CreateTransferBody, type TransferStatus,
} from "@/api";
import { PageHeader } from "@/components/PageHeader";
import { ProblemPanel } from "@/components/ProblemPanel";
import { StatusBadge } from "@/components/StatusBadge";
import { createIdempotencyKey } from "@/utils/idempotency";
import { formatMoney } from "@/utils/money";

type ScenarioKind = "success" | "risk" | "funds" | "replay";
interface DemoResult {
  readonly source: Account;
  readonly destination: Account;
  readonly transferId: string;
  readonly correlationId: string;
  readonly status: TransferStatus;
  readonly sourceLedgerCount: number;
  readonly destinationLedgerCount: number;
  readonly notificationCount: number;
  readonly replayed?: boolean;
  readonly replayTransferId?: string;
}

const scenarios: { kind: ScenarioKind; title: string; detail: string; expected: string }[] = [
  { kind: "success", title: "Successful transfer", detail: "Fund two accounts and follow reservation, risk approval, and settlement.", expected: "COMPLETED · source 874.50 · destination 225.50" },
  { kind: "risk", title: "Risk rejection + compensation", detail: "Use the deterministic blocked marker after funds are reserved.", expected: "COMPENSATING → REJECTED · source restored" },
  { kind: "funds", title: "Insufficient funds", detail: "Submit more than the source balance and observe direct rejection.", expected: "REJECTED · no balance mutation" },
  { kind: "replay", title: "Idempotency replay", detail: "Repeat the exact body and key to prove no duplicate transfer.", expected: "Same transfer ID · replay header true" },
];

function delay(milliseconds: number, signal: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    const timer = window.setTimeout(resolve, milliseconds);
    signal.addEventListener("abort", () => { window.clearTimeout(timer); reject(new DOMException("UI demonstration stopped", "AbortError")); }, { once: true });
  });
}

async function pollTerminal(transferId: string, signal: AbortSignal, onStatus: (status: TransferStatus) => void) {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    const result = await getTransfer(transferId, signal);
    onStatus(result.data.status);
    if (["COMPLETED", "REJECTED", "EXPIRED"].includes(result.data.status)) return result.data;
    await delay(Math.min(800 + attempt * 120, 2_000), signal);
  }
  throw new Error("The bounded demo poll ended before a terminal state was observed.");
}

export function DemoLabPage() {
  const controller = useRef<AbortController | null>(null);
  const [running, setRunning] = useState<ScenarioKind | null>(null);
  const [steps, setSteps] = useState<string[]>([]);
  const [status, setStatus] = useState<TransferStatus | null>(null);
  const [result, setResult] = useState<DemoResult | null>(null);
  const [error, setError] = useState<unknown>(null);

  const run = async (kind: ScenarioKind) => {
    controller.current?.abort();
    const active = new AbortController();
    controller.current = active;
    setRunning(kind); setSteps([]); setStatus(null); setResult(null); setError(null);
    const add = (message: string) => setSteps((current) => [...current, message]);
    try {
      const suffix = crypto.randomUUID().slice(0, 8);
      add("Creating isolated source and destination accounts");
      const [sourceCreated, destinationCreated] = await Promise.all([
        createAccount({ ownerReference: `demo-${kind}-source-${suffix}`, currency: "EUR" }),
        createAccount({ ownerReference: `demo-${kind}-destination-${suffix}`, currency: "EUR" }),
      ]);
      if (active.signal.aborted) throw new DOMException("UI demonstration stopped", "AbortError");
      const sourceFunding = kind === "funds" ? "50.00" : "1000.00";
      add(`Funding source with ${sourceFunding} and destination with 100.00`);
      await Promise.all([
        fundAccount(sourceCreated.data.accountId, { amount: sourceFunding, reference: `demo-fund-source-${suffix}` }),
        fundAccount(destinationCreated.data.accountId, { amount: "100.00", reference: `demo-fund-destination-${suffix}` }),
      ]);
      const body: CreateTransferBody = {
        sourceAccountId: sourceCreated.data.accountId,
        destinationAccountId: destinationCreated.data.accountId,
        amount: "125.50",
        currency: "EUR",
        reference: kind === "risk" ? `demo-RISK-REJECT-${suffix}` : `demo-${kind}-${suffix}`,
      };
      const idempotencyKey = createIdempotencyKey(`demo-${kind}`);
      add("Submitting transfer with a preserved cryptographic idempotency key");
      const accepted = await createTransfer(body, idempotencyKey);
      let replayed = false;
      let replayTransferId: string | undefined;
      if (kind === "replay") {
        add("Replaying the exact body and idempotency key");
        const replay = await createTransfer(body, idempotencyKey);
        replayed = replay.replayed;
        replayTransferId = replay.data.transferId;
      }
      add("Following persisted state until terminal");
      const terminal = await pollTerminal(accepted.data.transferId, active.signal, setStatus);
      add(`Observed terminal state ${terminal.status}; loading balances, ledgers, and notification`);
      const [source, destination, sourceLedger, destinationLedger, notifications] = await Promise.all([
        getAccount(sourceCreated.data.accountId, active.signal),
        getAccount(destinationCreated.data.accountId, active.signal),
        getLedger(sourceCreated.data.accountId, 0, 100, active.signal),
        getLedger(destinationCreated.data.accountId, 0, 100, active.signal),
        listNotifications({ transferId: terminal.transferId, size: 20 }, active.signal),
      ]);
      setResult({ source: source.data, destination: destination.data, transferId: terminal.transferId, correlationId: terminal.correlationId, status: terminal.status, sourceLedgerCount: sourceLedger.data.totalElements, destinationLedgerCount: destinationLedger.data.totalElements, notificationCount: notifications.data.totalElements, replayed, replayTransferId });
      add("Evidence collected from all service-owned read APIs");
    } catch (caught) {
      if (!(caught instanceof DOMException && caught.name === "AbortError")) setError(caught);
    } finally {
      setRunning(null);
      controller.current = null;
    }
  };

  return (
    <>
      <PageHeader eyebrow="Admin · guided real workflow" title="Demo Lab" description="Run isolated scenarios against Keycloak, the Gateway, Kafka, Redis, and service-owned PostgreSQL state." />
      <div className="notice notice--warning">Stopping the UI only stops polling. It never attempts to cancel a backend transfer and no global reset endpoint exists.</div>
      <section className="scenario-grid">
        {scenarios.map((scenario) => <article className="panel scenario-card" key={scenario.kind}><FlaskConical aria-hidden="true" /><h2>{scenario.title}</h2><p>{scenario.detail}</p><small>Expected: {scenario.expected}</small><button className="button button--primary" type="button" disabled={running !== null} onClick={() => void run(scenario.kind)}><Play aria-hidden="true" /> Run scenario</button></article>)}
      </section>
      {(running || steps.length > 0 || error || result) && <section className="panel demo-run" aria-live="polite">
        <div className="section-heading"><div><span className="eyebrow">Live orchestration</span><h2>{running ? "Scenario in progress" : result ? "Scenario complete" : "Scenario stopped"}</h2></div>{running && <button className="button button--secondary" type="button" onClick={() => controller.current?.abort()}><Square aria-hidden="true" /> Stop following</button>}</div>
        {status && <p>Latest transfer state: <StatusBadge status={status} /></p>}
        <ol className="step-list">{steps.map((step, index) => <li key={`${index}-${step}`}><CheckCircle2 aria-hidden="true" /> {step}</li>)}</ol>
        {error !== null && <ProblemPanel error={error} />}
        {result && <div className="demo-evidence">
          <div><span>Final transfer</span><StatusBadge status={result.status} /><Link to={`/transfers/${result.transferId}`}>{result.transferId}</Link></div>
          <div><span>Source final</span><strong>{formatMoney(result.source.availableBalance, result.source.currency)}</strong><Link to={`/accounts/${result.source.accountId}`}>{result.sourceLedgerCount} ledger entries</Link></div>
          <div><span>Destination final</span><strong>{formatMoney(result.destination.availableBalance, result.destination.currency)}</strong><Link to={`/accounts/${result.destination.accountId}`}>{result.destinationLedgerCount} ledger entries</Link></div>
          <div><span>Terminal records</span><strong>{result.notificationCount}</strong><Link to={`/notifications?transferId=${result.transferId}`}>Inspect notifications</Link></div>
          {result.replayTransferId && <div><span>Replay proof</span><strong>{result.replayed ? "Header confirmed" : "Header missing"}</strong><code>Same ID: {String(result.replayTransferId === result.transferId)}</code></div>}
        </div>}
      </section>}
    </>
  );
}
