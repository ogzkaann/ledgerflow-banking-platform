import type { AuthProviderProps } from "react-oidc-context";
import { WebStorageStateStore, type User } from "oidc-client-ts";
import { appConfig } from "@/config";

function restoreRoute(user: User | undefined): void {
  const state = user?.state;
  const returnTo =
    state && typeof state === "object" && "returnTo" in state
      ? (state as { returnTo?: unknown }).returnTo
      : undefined;
  const safeRoute =
    typeof returnTo === "string" && returnTo.startsWith("/") && !returnTo.startsWith("//")
      ? returnTo
      : "/";
  window.history.replaceState({}, document.title, safeRoute);
}

export const authConfig: AuthProviderProps = {
  authority: appConfig.oidc.authority,
  client_id: appConfig.oidc.clientId,
  redirect_uri: appConfig.oidc.redirectUri,
  post_logout_redirect_uri: appConfig.oidc.postLogoutRedirectUri,
  response_type: "code",
  scope: "openid",
  automaticSilentRenew: true,
  monitorSession: true,
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  stateStore: new WebStorageStateStore({ store: window.sessionStorage }),
  onSigninCallback: restoreRoute,
};
