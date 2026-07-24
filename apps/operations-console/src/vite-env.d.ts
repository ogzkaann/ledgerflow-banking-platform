/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_GATEWAY_READINESS_URL?: string;
  readonly VITE_OIDC_AUTHORITY?: string;
  readonly VITE_OIDC_CLIENT_ID?: string;
  readonly VITE_OIDC_REDIRECT_URI?: string;
  readonly VITE_OIDC_POST_LOGOUT_REDIRECT_URI?: string;
  readonly VITE_ENABLE_DEMO_FUNDING?: string;
  readonly VITE_ENVIRONMENT?: string;
  readonly VITE_GRAFANA_URL?: string;
  readonly VITE_KIBANA_URL?: string;
  readonly VITE_KEYCLOAK_ACCOUNT_URL?: string;
  readonly VITE_GITHUB_URL?: string;
  readonly VITE_DOCS_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
