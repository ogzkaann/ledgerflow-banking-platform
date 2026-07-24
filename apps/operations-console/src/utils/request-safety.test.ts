import { parseProblemDetail } from "@/api/problem";
import { validCorrelationId } from "./format";
import { createIdempotentDraft } from "./idempotency";

describe("request safety", () => {
  it("validates bounded correlation identifiers", () => {
    expect(validCorrelationId("corr.demo:2026-01")).toBe(true);
    expect(validCorrelationId("contains whitespace")).toBe(false);
    expect(validCorrelationId("x".repeat(65))).toBe(false);
  });

  it("retains an idempotency key across a retry draft", () => {
    const first = createIdempotentDraft({ amount: "12.50" });
    const replay = createIdempotentDraft(first.payload, first.key);
    expect(replay.key).toBe(first.key);
    expect(replay.payload).toEqual(first.payload);
  });

  it("parses actionable ProblemDetail fields and safe fallbacks", () => {
    expect(parseProblemDetail({ title: "Invalid", status: 422, detail: "Mismatch", errors: [{ field: "currency", message: "must match" }] }, 422)).toMatchObject({ title: "Invalid", status: 422, errors: [{ field: "currency", message: "must match" }] });
    expect(parseProblemDetail("html", 503)).toMatchObject({ title: "Service unavailable", status: 503 });
  });
});
