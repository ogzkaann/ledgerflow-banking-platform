import { useMutation } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createAccount } from "@/api";
import { PageHeader } from "@/components/PageHeader";
import { ProblemPanel } from "@/components/ProblemPanel";

export function CreateAccountPage() {
  const navigate = useNavigate();
  const [ownerReference, setOwnerReference] = useState("");
  const [currency, setCurrency] = useState<"EUR" | "USD" | "GBP">("EUR");
  const mutation = useMutation({
    mutationFn: createAccount,
    onSuccess: (result) => navigate(`/accounts/${result.data.accountId}`, { state: { created: true, correlationId: result.correlationId } }),
  });
  const trimmed = ownerReference.trim();
  const validation = trimmed.length === 0 ? "Owner reference is required." : trimmed.length > 120 ? "Use 120 characters or fewer." : null;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!validation) mutation.mutate({ ownerReference: trimmed, currency });
  };
  return (
    <>
      <PageHeader eyebrow="Operator action" title="Create an account" description="Create a real account record through the secured API Gateway." />
      <section className="panel form-panel">
        <form onSubmit={submit} noValidate>
          <label>Owner reference<input value={ownerReference} maxLength={120} autoFocus onChange={(event) => setOwnerReference(event.target.value)} aria-describedby="owner-help" /></label>
          <p id="owner-help" className="field-help">A stable external reference; no customer identity model is introduced in this phase.</p>
          <label>Currency<select value={currency} onChange={(event) => setCurrency(event.target.value as typeof currency)}><option>EUR</option><option>USD</option><option>GBP</option></select></label>
          {validation && ownerReference.length > 0 && <p className="field-error">{validation}</p>}
          {mutation.error && <ProblemPanel error={mutation.error} />}
          <div className="form-actions"><Link className="button button--quiet" to="/accounts">Cancel</Link><button className="button button--primary" disabled={Boolean(validation) || mutation.isPending} type="submit">{mutation.isPending ? "Creating…" : "Create account"}</button></div>
        </form>
      </section>
    </>
  );
}
