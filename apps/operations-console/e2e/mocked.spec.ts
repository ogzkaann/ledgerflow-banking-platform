import { expect, test } from "@playwright/test";
import { completedHistory, installKeycloakDiscovery, installMockApi, installSession, transfer } from "./support";

test("protected routes initiate a Keycloak Authorization Code login", async ({ page }) => {
  await page.route("http://localhost:8090/realms/ledgerflow/.well-known/openid-configuration", installKeycloakDiscovery);
  await page.route("http://localhost:8090/realms/ledgerflow/protocol/openid-connect/auth**", (route) =>
    route.fulfill({ contentType: "text/html", body: "<h1>Keycloak login</h1>" }),
  );
  await page.goto("/accounts");
  await expect(page).toHaveURL(/localhost:8090.*openid-connect\/auth/);
  expect(page.url()).toContain("code_challenge_method=S256");
  expect(page.url()).toContain("response_type=code");
});

test("admin creates an account and uses local demo funding", async ({ page }) => {
  await installSession(page);
  await installMockApi(page);
  await page.goto("/accounts");
  await expect(page.getByText("recruiter-source")).toBeVisible();
  await page.getByRole("link", { name: /create account/i }).click();
  await page.getByLabel("Owner reference").fill("created-in-browser");
  await page.getByRole("button", { name: "Create account" }).click();
  await expect(page).toHaveURL(/\/accounts\/aaaaaaaa/);
  await expect(page.getByText("Account created successfully.")).toBeVisible();
  await page.getByLabel("Amount").fill("1000.00");
  await page.getByLabel("Unique reference").fill("browser-funding");
  await page.getByRole("button", { name: "Add demo funding" }).click();
  await expect(page.getByText(/Synthetic funding persisted/)).toBeVisible();
});

test("auditor has read access but no write controls", async ({ page }) => {
  await installSession(page, "ledgerflow-auditor");
  await installMockApi(page);
  await page.goto("/accounts");
  await expect(page.getByText("recruiter-source")).toBeVisible();
  await expect(page.getByRole("link", { name: /create account/i })).toHaveCount(0);
  await page.goto("/transfers/new");
  await expect(page).toHaveURL("/");
  await expect(page.getByText(/does not permit that write operation/)).toBeVisible();
});

test("operator submits a transfer and follows pending to completed", async ({ page }) => {
  const browserErrors: string[] = [];
  page.on("console", (message) => { if (message.type() === "error") browserErrors.push(message.text()); });
  await installSession(page, "ledgerflow-operator");
  await installMockApi(page);
  await page.goto("/transfers/new");
  await page.getByLabel("Source account").selectOption("11111111-1111-4111-8111-111111111111");
  await page.getByLabel("Destination account").selectOption("22222222-2222-4222-8222-222222222222");
  await page.getByLabel("Amount").fill("125.50");
  await page.getByLabel("Reference").fill("browser-invoice");
  await page.getByRole("button", { name: "Submit transfer" }).click();
  await expect(page).toHaveURL(new RegExp(`/transfers/${transfer.transferId}`));
  await expect(page.getByText("202 Accepted.")).toBeVisible();
  await expect(page.getByText("COMPLETED", { exact: true }).first()).toBeVisible({ timeout: 10_000 });
  for (const item of completedHistory) await expect(page.getByText(item.toStatus.replaceAll("_", " "), { exact: true }).first()).toBeVisible();
  expect(browserErrors).toEqual([]);
});

test("rejected history renders compensation without approved or settling steps", async ({ page }) => {
  await installSession(page);
  await installMockApi(page, { rejected: true });
  await page.goto(`/transfers/${transfer.transferId}`);
  await expect(page.getByText("COMPENSATING", { exact: true })).toBeVisible();
  await expect(page.getByText("REJECTED", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("RISK APPROVED", { exact: true })).toHaveCount(0);
  await expect(page.getByText("SETTLING", { exact: true })).toHaveCount(0);
});

test("notification records navigate to transfer detail", async ({ page }) => {
  await installSession(page);
  await installMockApi(page);
  await page.goto("/notifications");
  await expect(page.getByText("TRANSFER COMPLETED")).toBeVisible();
  await page.getByRole("link", { name: /33333333/ }).click();
  await expect(page).toHaveURL(new RegExp(`/transfers/${transfer.transferId}`));
});

test("responsive navigation and keyboard focus remain usable", async ({ page }) => {
  await installSession(page);
  await installMockApi(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  const menu = page.getByRole("button", { name: "Open navigation" });
  await expect(menu).toBeVisible();
  await menu.focus();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("navigation", { name: "Primary navigation" })).toBeVisible();
  await page.getByRole("link", { name: "Accounts" }).click();
  await expect(page).toHaveURL(/\/accounts/);
});
