import createClient, { type Middleware } from "openapi-fetch";
import type { paths as AccountPaths } from "./generated/account";
import type { paths as TransferPaths } from "./generated/transfer";
import type { paths as NotificationPaths } from "./generated/notification";
import { appConfig } from "@/config";

type AccessTokenProvider = () => string | undefined;
let accessTokenProvider: AccessTokenProvider = () => undefined;

export function setAccessTokenProvider(provider: AccessTokenProvider): () => void {
  accessTokenProvider = provider;
  return () => {
    accessTokenProvider = () => undefined;
  };
}

const authMiddleware: Middleware = {
  onRequest({ request }) {
    const token = accessTokenProvider();
    if (token) request.headers.set("Authorization", `Bearer ${token}`);
    if (!request.headers.has("X-Correlation-Id")) {
      request.headers.set("X-Correlation-Id", crypto.randomUUID());
    }
    return request;
  },
  onResponse({ response }) {
    if (response.status === 401) {
      window.dispatchEvent(new CustomEvent("ledgerflow:session-expired"));
    }
    return response;
  },
};

async function fetchWithTimeout(request: Request): Promise<Response> {
  const timeoutSignal = AbortSignal.timeout(appConfig.requestTimeoutMs);
  const signal = AbortSignal.any([request.signal, timeoutSignal]);
  return fetch(new Request(request, { signal }));
}

function configuredClient<Paths extends object>() {
  const client = createClient<Paths>({
    baseUrl: appConfig.apiBaseUrl,
    fetch: fetchWithTimeout,
  });
  client.use(authMiddleware);
  return client;
}

export const accountClient = configuredClient<AccountPaths>();
export const transferClient = configuredClient<TransferPaths>();
export const notificationClient = configuredClient<NotificationPaths>();
