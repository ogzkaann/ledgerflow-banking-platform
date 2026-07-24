import type { components as AccountComponents } from "./generated/account";
import type { components as TransferComponents } from "./generated/transfer";
import type { components as NotificationComponents } from "./generated/notification";
import { accountClient, notificationClient, transferClient } from "./clients";
import { ApiProblem, parseProblemDetail } from "./problem";
import { appConfig } from "@/config";

export type Account = AccountComponents["schemas"]["Account"];
export type AccountPage = AccountComponents["schemas"]["AccountPage"];
export type LedgerPage = AccountComponents["schemas"]["LedgerPage"];
export type FundingResult = AccountComponents["schemas"]["FundingResult"];
export type Transfer = TransferComponents["schemas"]["Transfer"];
export type TransferStatus = TransferComponents["schemas"]["TransferStatus"];
export type TransferPage = TransferComponents["schemas"]["TransferPage"];
export type TransferHistory = TransferComponents["schemas"]["TransferHistory"];
export type Notification = NotificationComponents["schemas"]["Notification"];
export type NotificationPage = NotificationComponents["schemas"]["NotificationPage"];

export interface ApiResponse<T> {
  readonly data: T;
  readonly correlationId: string | null;
  readonly replayed: boolean;
  readonly retryAfterSeconds: number | null;
}

async function unwrap<T>(
  result: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<ApiResponse<T>> {
  const { data, error, response } = await result;
  const correlationId = response.headers.get("X-Correlation-Id");
  const retryValue = response.headers.get("Retry-After");
  const retryAfterSeconds = retryValue && /^\d+$/.test(retryValue) ? Number(retryValue) : null;
  if (!response.ok || data === undefined) {
    throw new ApiProblem(
      parseProblemDetail(error, response.status),
      correlationId,
      retryAfterSeconds,
    );
  }
  return {
    data,
    correlationId,
    replayed: response.headers.get("Idempotency-Replayed") === "true",
    retryAfterSeconds,
  };
}

export interface AccountFilters {
  readonly page?: number;
  readonly size?: number;
  readonly status?: "ACTIVE" | "FROZEN" | "CLOSED";
  readonly currency?: "EUR" | "USD" | "GBP";
}

export function listAccounts(filters: AccountFilters, signal?: AbortSignal) {
  return unwrap(
    accountClient.GET("/api/v1/accounts", {
      params: { query: filters },
      signal,
    }),
  );
}

export function getAccount(accountId: string, signal?: AbortSignal) {
  return unwrap(
    accountClient.GET("/api/v1/accounts/{accountId}", {
      params: { path: { accountId } },
      signal,
    }),
  );
}

export function getLedger(accountId: string, page: number, size: number, signal?: AbortSignal) {
  return unwrap(
    accountClient.GET("/api/v1/accounts/{accountId}/ledger", {
      params: { path: { accountId }, query: { page, size } },
      signal,
    }),
  );
}

export function createAccount(body: { ownerReference: string; currency: string }) {
  return unwrap(
    accountClient.POST("/api/v1/accounts", {
      body,
    }),
  );
}

export function fundAccount(accountId: string, body: { amount: string; reference: string }) {
  return unwrap(
    accountClient.POST("/api/v1/accounts/{accountId}/test-funding", {
      params: { path: { accountId } },
      body,
    }),
  );
}

export interface TransferFilters {
  readonly page?: number;
  readonly size?: number;
  readonly status?: TransferStatus;
  readonly sourceAccountId?: string;
  readonly destinationAccountId?: string;
  readonly reference?: string;
  readonly correlationId?: string;
  readonly createdFrom?: string;
  readonly createdTo?: string;
}

export function listTransfers(filters: TransferFilters, signal?: AbortSignal) {
  return unwrap(
    transferClient.GET("/api/v1/transfers", {
      params: { query: filters },
      signal,
    }),
  );
}

export interface CreateTransferBody {
  readonly sourceAccountId: string;
  readonly destinationAccountId: string;
  readonly amount: string;
  readonly currency: "EUR" | "USD" | "GBP";
  readonly reference: string;
}

export function createTransfer(
  body: CreateTransferBody,
  idempotencyKey: string,
  correlationId: string = crypto.randomUUID(),
) {
  return unwrap(
    transferClient.POST("/api/v1/transfers", {
      params: {
        header: {
          "Idempotency-Key": idempotencyKey,
          "X-Correlation-Id": correlationId,
        },
      },
      body,
    }),
  );
}

export function getTransfer(transferId: string, signal?: AbortSignal) {
  return unwrap(
    transferClient.GET("/api/v1/transfers/{transferId}", {
      params: { path: { transferId } },
      signal,
    }),
  );
}

export function getTransferHistory(transferId: string, signal?: AbortSignal) {
  return unwrap(
    transferClient.GET("/api/v1/transfers/{transferId}/history", {
      params: { path: { transferId } },
      signal,
    }),
  );
}

export interface NotificationFilters {
  readonly page?: number;
  readonly size?: number;
  readonly transferId?: string;
  readonly type?: "TRANSFER_COMPLETED" | "TRANSFER_REJECTED";
}

export function listNotifications(filters: NotificationFilters, signal?: AbortSignal) {
  return unwrap(
    notificationClient.GET("/api/v1/notifications", {
      params: { query: filters },
      signal,
    }),
  );
}

export async function getGatewayReadiness(signal?: AbortSignal): Promise<boolean> {
  try {
    const response = await fetch(`${appConfig.apiBaseUrl}/actuator/health/readiness`, {
      signal: signal
        ? AbortSignal.any([signal, AbortSignal.timeout(appConfig.requestTimeoutMs)])
        : AbortSignal.timeout(appConfig.requestTimeoutMs),
      headers: { "X-Correlation-Id": crypto.randomUUID() },
    });
    return response.ok;
  } catch {
    return false;
  }
}
