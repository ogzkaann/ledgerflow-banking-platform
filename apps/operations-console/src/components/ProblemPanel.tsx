import { AlertTriangle, RotateCcw } from "lucide-react";
import { ApiProblem } from "@/api/problem";
import { CopyButton } from "./CopyButton";

interface ProblemPanelProps {
  readonly error: unknown;
  readonly onRetry?: () => void;
}

export function ProblemPanel({ error, onRetry }: ProblemPanelProps) {
  const problem =
    error instanceof ApiProblem
      ? error
      : new ApiProblem(
          {
            title: "Backend unavailable",
            status: 0,
            detail:
              error instanceof Error
                ? error.message
                : "The console could not reach the LedgerFlow API Gateway.",
          },
          null,
          null,
        );

  return (
    <section className="problem-panel" role="alert">
      <AlertTriangle aria-hidden="true" />
      <div>
        <h2>{problem.problem.title}</h2>
        <p>{problem.problem.detail ?? problem.message}</p>
        {problem.retryAfterSeconds !== null && (
          <p>
            The Gateway requested a retry after <strong>{problem.retryAfterSeconds} seconds</strong>.
          </p>
        )}
        {problem.problem.errors?.map((item) => (
          <p key={`${item.field ?? "request"}-${item.message ?? "invalid"}`}>
            <strong>{item.field ?? "Request"}:</strong> {item.message ?? "Invalid value"}
          </p>
        ))}
        {problem.correlationId && (
          <div className="technical-inline">
            <span>Correlation</span>
            <code>{problem.correlationId}</code>
            <CopyButton value={problem.correlationId} label="Copy correlation ID" />
          </div>
        )}
        {onRetry && (
          <button className="button button--secondary" type="button" onClick={onRetry}>
            <RotateCcw aria-hidden="true" />
            Retry
          </button>
        )}
      </div>
    </section>
  );
}
