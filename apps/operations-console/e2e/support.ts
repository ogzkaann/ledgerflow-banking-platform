import type { Page, Route } from "@playwright/test";

export const firstAccount = {
  accountId: "11111111-1111-4111-8111-111111111111",
  ownerReference: "recruiter-source",
  currency: "EUR",
  status: "ACTIVE",
  availableBalance: "1000.00",
  reservedBalance: "0.00",
  version: 2,
  createdAt: "2026-07-24T12:00:00Z",
  updatedAt: "2026-07-24T12:05:00Z",
};
export const secondAccount = {
  ...firstAccount,
  accountId: "22222222-2222-4222-8222-222222222222",
  ownerReference: "recruiter-destination",
  availableBalance: "100.00",
};
export const transfer = {
  transferId: "33333333-3333-4333-8333-333333333333",
  sourceAccountId: firstAccount.accountId,
  destinationAccountId: secondAccount.accountId,
  amount: "125.50",
  currency: "EUR",
  reference: "browser-invoice",
  status: "COMPLETED",
  correlationId: "corr-browser-001",
  createdAt: "2026-07-24T12:10:00Z",
  updatedAt: "2026-07-24T12:10:05Z",
  version: 4,
};
export const completedHistory = [
  transition("1", null, "PENDING", 1),
  transition("2", "PENDING", "FUNDS_RESERVED", 2),
  transition("3", "FUNDS_RESERVED", "RISK_APPROVED", 3),
  transition("4", "RISK_APPROVED", "SETTLING", 4),
  transition("5", "SETTLING", "COMPLETED", 5),
];

function transition(id: string, fromStatus: string | null, toStatus: string, sequence: number) {
  return {
    transitionId: `40000000-0000-4000-8000-00000000000${id}`,
    fromStatus,
    toStatus,
    reason: toStatus.toLowerCase().replaceAll("_", "-"),
    occurredAt: `2026-07-24T12:10:0${sequence}Z`,
    sequence,
  };
}

export async function installSession(page: Page, role = "ledgerflow-admin") {
  const expiresAt = Math.floor(Date.now() / 1000) + 3_600;
  await page.addInitScript(
    ({ expiresAt, role }) => {
      sessionStorage.setItem(
        "oidc.user:http://localhost:8090/realms/ledgerflow:ledgerflow-spa",
        JSON.stringify({
          access_token: "browser-test-token",
          token_type: "Bearer",
          scope: "openid profile email",
          expires_at: expiresAt,
          profile: {
            sub: "browser-user",
            preferred_username: role.replace("ledgerflow-", ""),
            realm_access: { roles: [role] },
          },
        }),
      );
    },
    { expiresAt, role },
  );
}

function pageResult(content: unknown[], size = 20) {
  return { content, page: 0, size, totalElements: content.length, totalPages: content.length ? 1 : 0 };
}

export async function installMockApi(page: Page, options?: { rejected?: boolean }) {
  let detailReads = 0;
  await page.route("http://localhost:8080/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const headers = { "content-type": "application/json", "x-correlation-id": "corr-browser-response" };
    const json = (value: unknown, status = 200, extra: Record<string, string> = {}) =>
      route.fulfill({ status, headers: { ...headers, ...extra }, body: JSON.stringify(value) });

    if (path === "/actuator/health/readiness") return json({ status: "UP" });
    if (path === "/api/v1/accounts" && request.method() === "GET") return json(pageResult([firstAccount, secondAccount]));
    if (path === "/api/v1/accounts" && request.method() === "POST") return json({ ...firstAccount, accountId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", ownerReference: "created-in-browser" }, 201);
    if (/\/api\/v1\/accounts\/[^/]+\/test-funding$/.test(path)) return json({ account: firstAccount, ledgerEntry: { ledgerEntryId: "60000000-0000-4000-8000-000000000001", accountId: firstAccount.accountId, type: "CREDIT", amount: "1000.00", currency: "EUR", reference: "browser-funding", createdAt: "2026-07-24T12:00:00Z" } }, 201);
    if (/\/api\/v1\/accounts\/[^/]+\/ledger$/.test(path)) return json(pageResult([]));
    if (/\/api\/v1\/accounts\/[^/]+$/.test(path)) return json(path.includes(secondAccount.accountId) ? secondAccount : firstAccount);
    if (path === "/api/v1/transfers" && request.method() === "POST") {
      const replay = request.headers()["x-browser-replay"] === "true" || detailReads > 1;
      return json({ ...transfer, status: "PENDING", version: 0 }, 202, replay ? { "idempotency-replayed": "true" } : {});
    }
    if (path === "/api/v1/transfers" && request.method() === "GET") return json(pageResult([transfer]));
    if (/\/api\/v1\/transfers\/[^/]+\/history$/.test(path)) {
      if (options?.rejected) return json([
        transition("1", null, "PENDING", 1),
        transition("2", "PENDING", "FUNDS_RESERVED", 2),
        transition("3", "FUNDS_RESERVED", "COMPENSATING", 3),
        transition("4", "COMPENSATING", "REJECTED", 4),
      ]);
      return json(completedHistory);
    }
    if (/\/api\/v1\/transfers\/[^/]+$/.test(path)) {
      detailReads += 1;
      if (options?.rejected) return json({ ...transfer, status: "REJECTED", reference: "RISK-REJECT-browser", version: 3 });
      const status = detailReads === 1 ? "PENDING" : detailReads === 2 ? "FUNDS_RESERVED" : "COMPLETED";
      return json({ ...transfer, status, version: detailReads });
    }
    if (path === "/api/v1/notifications") return json(pageResult([{
      notificationId: "50000000-0000-4000-8000-000000000001",
      transferId: transfer.transferId,
      eventId: "50000000-0000-4000-8000-000000000002",
      type: options?.rejected ? "TRANSFER_REJECTED" : "TRANSFER_COMPLETED",
      finalTransferStatus: options?.rejected ? "REJECTED" : "COMPLETED",
      correlationId: transfer.correlationId,
      messageTemplateKey: options?.rejected ? "transfer.rejected" : "transfer.completed",
      createdAt: "2026-07-24T12:10:06Z",
    }]));
    return json({ title: "Unhandled browser fixture", status: 500, detail: `${request.method()} ${path}` }, 500);
  });
}

export async function installKeycloakDiscovery(route: Route) {
  const issuer = "http://localhost:8090/realms/ledgerflow";
  await route.fulfill({
    contentType: "application/json",
    body: JSON.stringify({
      issuer,
      authorization_endpoint: `${issuer}/protocol/openid-connect/auth`,
      token_endpoint: `${issuer}/protocol/openid-connect/token`,
      end_session_endpoint: `${issuer}/protocol/openid-connect/logout`,
      jwks_uri: `${issuer}/protocol/openid-connect/certs`,
      response_types_supported: ["code"],
      code_challenge_methods_supported: ["S256"],
    }),
  });
}
