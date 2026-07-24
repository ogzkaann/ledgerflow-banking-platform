import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import { AccountsPage } from "./AccountsPage";
import { renderConsole } from "@/test/render";
import { server } from "@/test/server";
import { setMockRole } from "@/test/auth-mock";
import { axe } from "jest-axe";

describe("AccountsPage", () => {
  it("renders contract-shaped account state and admin create action", async () => {
    setMockRole("ledgerflow-admin");
    const view = renderConsole(<AccountsPage />, "/accounts");
    expect(await screen.findByText("portfolio-source")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /create account/i })).toBeInTheDocument();
    expect(screen.getByText("EUR 1,000.00")).toBeInTheDocument();
    expect((await axe(view.container)).violations).toEqual([]);
  });

  it("keeps an auditor read-only", async () => {
    setMockRole("ledgerflow-auditor");
    renderConsole(<AccountsPage />, "/accounts");
    expect(await screen.findByText("portfolio-source")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /create account/i })).not.toBeInTheDocument();
  });

  it("shows empty and ProblemDetail states", async () => {
    server.use(http.get("http://localhost:8080/api/v1/accounts", () => HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })));
    const view = renderConsole(<AccountsPage />, "/accounts");
    expect(await screen.findByText("No matching accounts")).toBeInTheDocument();
    view.unmount();
    server.use(http.get("http://localhost:8080/api/v1/accounts", () => HttpResponse.json({ title: "Rate limit exceeded", status: 429, detail: "Try later" }, { status: 429, headers: { "Retry-After": "5", "X-Correlation-Id": "corr-rate-1" } })));
    renderConsole(<AccountsPage />, "/accounts");
    expect(await screen.findByText("Rate limit exceeded")).toBeInTheDocument();
    expect(screen.getByText(/5 seconds/)).toBeInTheDocument();
    expect(screen.getByText("corr-rate-1")).toBeInTheDocument();
  });
});
