type ConfigKey =
  | "VITE_API_BASE_URL"
  | "VITE_GATEWAY_READINESS_URL"
  | "VITE_OIDC_AUTHORITY"
  | "VITE_OIDC_CLIENT_ID"
  | "VITE_OIDC_REDIRECT_URI"
  | "VITE_OIDC_POST_LOGOUT_REDIRECT_URI"
  | "VITE_ENABLE_DEMO_FUNDING"
  | "VITE_ENVIRONMENT"
  | "VITE_GRAFANA_URL"
  | "VITE_KIBANA_URL"
  | "VITE_KEYCLOAK_ACCOUNT_URL"
  | "VITE_GITHUB_URL"
  | "VITE_DOCS_BASE_URL";

function value(name: ConfigKey, fallback: string): string {
  const configured: string | undefined = import.meta.env[name];
  return configured && configured.trim() ? configured.trim() : fallback;
}

export const appConfig = {
  apiBaseUrl: value("VITE_API_BASE_URL", "http://localhost:8080"),
  gatewayReadinessUrl: value("VITE_GATEWAY_READINESS_URL", "/gateway-readiness"),
  environment: value("VITE_ENVIRONMENT", "local"),
  enableDemoFunding: value("VITE_ENABLE_DEMO_FUNDING", "false") === "true",
  oidc: {
    authority: value("VITE_OIDC_AUTHORITY", "http://localhost:8090/realms/ledgerflow"),
    clientId: value("VITE_OIDC_CLIENT_ID", "ledgerflow-spa"),
    redirectUri: value("VITE_OIDC_REDIRECT_URI", "http://localhost:5173/oidc/callback"),
    postLogoutRedirectUri: value("VITE_OIDC_POST_LOGOUT_REDIRECT_URI", "http://localhost:5173/"),
  },
  links: {
    grafana: value("VITE_GRAFANA_URL", "http://localhost:3000"),
    kibana: value("VITE_KIBANA_URL", "http://localhost:5601"),
    keycloakAccount: value(
      "VITE_KEYCLOAK_ACCOUNT_URL",
      "http://localhost:8090/realms/ledgerflow/account",
    ),
    github: value(
      "VITE_GITHUB_URL",
      "https://github.com/ogzkaann/ledgerflow-banking-platform",
    ),
    docsBase: value(
      "VITE_DOCS_BASE_URL",
      "https://github.com/ogzkaann/ledgerflow-banking-platform/blob/main",
    ),
  },
  requestTimeoutMs: 10_000,
} as const;
