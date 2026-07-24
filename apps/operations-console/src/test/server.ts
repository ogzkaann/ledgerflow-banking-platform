import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { accountFixture, historyFixture, notificationFixture, transferFixture } from "./fixtures";

const base = "http://localhost:8080";

export const handlers = [
  http.get(`${base}/api/v1/accounts`, () => HttpResponse.json({ content: [accountFixture], page: 0, size: 20, totalElements: 1, totalPages: 1 })),
  http.get(`${base}/api/v1/accounts/:accountId`, () => HttpResponse.json(accountFixture)),
  http.get(`${base}/api/v1/accounts/:accountId/ledger`, () => HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })),
  http.get(`${base}/api/v1/transfers`, () => HttpResponse.json({ content: [transferFixture], page: 0, size: 20, totalElements: 1, totalPages: 1 })),
  http.get(`${base}/api/v1/transfers/:transferId`, () => HttpResponse.json(transferFixture)),
  http.get(`${base}/api/v1/transfers/:transferId/history`, () => HttpResponse.json(historyFixture)),
  http.get(`${base}/api/v1/notifications`, () => HttpResponse.json({ content: [notificationFixture], page: 0, size: 20, totalElements: 1, totalPages: 1 })),
];

export const server = setupServer(...handlers);
