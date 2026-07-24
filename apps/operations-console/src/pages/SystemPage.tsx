import { useQuery } from "@tanstack/react-query";
import { ExternalLink, KeyRound, Network, RefreshCw, ShieldCheck } from "lucide-react";
import { getGatewayReadiness } from "@/api";
import { appConfig } from "@/config";
import { PageHeader } from "@/components/PageHeader";
import { StatusBadge } from "@/components/StatusBadge";

const architecture = [
  ["Account Service", "Owns accounts, reservations, settlement, compensation, and immutable ledger entries."],
  ["Transfer Service", "Owns intake, state-machine transitions, durable idempotency, and its transactional outbox."],
  ["Risk Service", "Applies deterministic rules privately from Kafka events; it has no public Gateway route."],
  ["Notification Service", "Persists one terminal demo record through an idempotent Kafka consumer."],
  ["API Gateway", "Validates issuer, audience, roles, CORS, correlation, and fail-closed Redis rate limits."],
];

export function SystemPage() {
  const readiness = useQuery({ queryKey: ["gateway-readiness"], queryFn: ({ signal }) => getGatewayReadiness(signal), refetchInterval: 15_000 });
  const docs = appConfig.links.docsBase;
  return (
    <>
      <PageHeader eyebrow="Technical proof" title="System" description="The boundaries, delivery semantics, and live entry-point evidence behind this console." />
      <section className="metric-grid metric-grid--system">
        <article className="metric-card"><Network aria-hidden="true" /><span>Gateway readiness</span><StatusBadge status={readiness.data ? "HEALTHY" : "UNAVAILABLE"} /><button className="icon-button" onClick={() => void readiness.refetch()} aria-label="Refresh Gateway readiness"><RefreshCw aria-hidden="true" /></button></article>
        <article className="metric-card"><ShieldCheck aria-hidden="true" /><span>Browser flow</span><strong>Code + PKCE</strong></article>
        <article className="metric-card"><KeyRound aria-hidden="true" /><span>Current environment</span><strong>{appConfig.environment}</strong></article>
      </section>
      <section className="system-grid">
        {architecture.map(([title, detail]) => <article className="panel" key={title}><h2>{title}</h2><p>{detail}</p></article>)}
      </section>
      <section className="panel"><span className="eyebrow">Delivery guarantees</span><h2>At-least-once transport, exactly-once business effect</h2><p>Each service commits business state and an outbox record in one PostgreSQL transaction. Relay delivery may repeat, so consumers persist processed event IDs and apply the business change atomically. Transfer intake separately persists the idempotency key and request fingerprint.</p></section>
      <section className="panel link-grid" aria-label="Technical resources">
        <a href={appConfig.links.grafana} target="_blank" rel="noreferrer">Grafana <ExternalLink aria-hidden="true" /></a>
        <a href={appConfig.links.kibana} target="_blank" rel="noreferrer">Kibana <ExternalLink aria-hidden="true" /></a>
        <a href={appConfig.links.keycloakAccount} target="_blank" rel="noreferrer">Keycloak account <ExternalLink aria-hidden="true" /></a>
        <a href={`${docs}/contracts/openapi`} target="_blank" rel="noreferrer">OpenAPI contracts <ExternalLink aria-hidden="true" /></a>
        <a href={`${docs}/contracts/asyncapi/ledgerflow-events.yaml`} target="_blank" rel="noreferrer">AsyncAPI contract <ExternalLink aria-hidden="true" /></a>
        <a href={`${docs}/docs/architecture/system-design.md`} target="_blank" rel="noreferrer">Architecture <ExternalLink aria-hidden="true" /></a>
        <a href={appConfig.links.github} target="_blank" rel="noreferrer">GitHub repository <ExternalLink aria-hidden="true" /></a>
      </section>
    </>
  );
}
