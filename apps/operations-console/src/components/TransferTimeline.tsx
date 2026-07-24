import type { TransferHistory, TransferStatus } from "@/api";
import { formatDateTime } from "@/utils/format";

const terminalStatuses: TransferStatus[] = ["COMPLETED", "REJECTED", "EXPIRED"];

export function isTerminalStatus(status: TransferStatus): boolean {
  return terminalStatuses.includes(status);
}

export function TransferTimeline({
  history,
  currentStatus,
}: {
  readonly history: TransferHistory[];
  readonly currentStatus: TransferStatus;
}) {
  return (
    <ol className="timeline" aria-label="Transfer state history" aria-live="polite">
      {history.map((transition) => (
        <li
          key={transition.transitionId}
          className={`timeline__item timeline__item--${transition.toStatus.toLowerCase()}`}
        >
          <span className="timeline__marker" aria-hidden="true" />
          <div>
            <div className="timeline__heading">
              <strong>{transition.toStatus.replaceAll("_", " ")}</strong>
              <time dateTime={transition.occurredAt}>{formatDateTime(transition.occurredAt)}</time>
            </div>
            <p>{transition.reason}</p>
            <code>Sequence {transition.sequence}</code>
          </div>
        </li>
      ))}
      {!isTerminalStatus(currentStatus) && (
        <li className="timeline__item timeline__item--waiting">
          <span className="timeline__marker" aria-hidden="true" />
          <div>
            <strong>Workflow processing</strong>
            <p>Waiting for the next persisted state transition.</p>
          </div>
        </li>
      )}
    </ol>
  );
}
