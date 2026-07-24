export interface ProblemDetail {
  readonly type?: string;
  readonly title: string;
  readonly status: number;
  readonly detail?: string;
  readonly instance?: string;
  readonly errors?: readonly { readonly field?: string; readonly message?: string }[];
}

export class ApiProblem extends Error {
  readonly problem: ProblemDetail;
  readonly correlationId: string | null;
  readonly retryAfterSeconds: number | null;

  constructor(
    problem: ProblemDetail,
    correlationId: string | null,
    retryAfterSeconds: number | null,
  ) {
    super(problem.detail ?? problem.title);
    this.name = "ApiProblem";
    this.problem = problem;
    this.correlationId = correlationId;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export function parseProblemDetail(value: unknown, status: number): ProblemDetail {
  if (!value || typeof value !== "object") {
    return {
      title: status >= 500 ? "Service unavailable" : "Request failed",
      status,
      detail: "The service returned an unreadable error response.",
    };
  }
  const item = value as Record<string, unknown>;
  const errors = Array.isArray(item.errors)
    ? item.errors
        .filter((entry): entry is Record<string, unknown> => Boolean(entry) && typeof entry === "object")
        .map((entry) => ({
          field: typeof entry.field === "string" ? entry.field : undefined,
          message: typeof entry.message === "string" ? entry.message : undefined,
        }))
    : undefined;
  return {
    type: typeof item.type === "string" ? item.type : undefined,
    title: typeof item.title === "string" ? item.title : "Request failed",
    status: typeof item.status === "number" ? item.status : status,
    detail: typeof item.detail === "string" ? item.detail : undefined,
    instance: typeof item.instance === "string" ? item.instance : undefined,
    errors,
  };
}
