import { expect, test, type Page } from "@playwright/test";

test.skip(process.env.LEDGERFLOW_REAL_E2E !== "true", "Requires the complete local LedgerFlow stack");

async function signIn(page: Page, username: string, password: string) {
  await page.goto("/");
  await page.getByLabel(/username or email/i).fill(username);
  await page.getByLabel(/password/i).fill(password);
  await page.getByRole("button", { name: /sign in/i }).click();
  await expect(page).toHaveURL("http://localhost:5173/");
}

test("real Keycloak, Gateway, Kafka, Redis, and PostgreSQL complete a transfer", async ({ page }) => {
  const username = process.env.LEDGERFLOW_ADMIN_USERNAME ?? "admin";
  const password = process.env.LEDGERFLOW_ADMIN_PASSWORD;
  if (!password) throw new Error("LEDGERFLOW_ADMIN_PASSWORD is required for real browser verification");
  const browserErrors: string[] = [];
  page.on("console", (message) => { if (message.type() === "error") browserErrors.push(message.text()); });
  await signIn(page, username, password);
  await expect(page.getByRole("heading", { name: "See the workflow, not a simulation" })).toBeVisible();
  await page.goto("/demo");
  await page.getByRole("button", { name: "Run scenario" }).first().click();
  await expect(page.getByRole("heading", { name: "Scenario complete" })).toBeVisible({ timeout: 90_000 });
  await expect(page.getByText("COMPLETED", { exact: true }).last()).toBeVisible();
  await expect(page.getByText("EUR 874.50")).toBeVisible();
  await expect(page.getByText("EUR 225.50")).toBeVisible();
  expect(browserErrors).toEqual([]);
});
