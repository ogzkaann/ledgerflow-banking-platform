import { expect, test, type Page, type TestInfo } from "@playwright/test";

test.skip(process.env.LEDGERFLOW_REAL_E2E !== "true", "Requires the complete local LedgerFlow stack");
test.setTimeout(6 * 60_000);

async function signIn(page: Page, username: string, password: string, testInfo: TestInfo) {
  await page.goto("/");
  await page.screenshot({ path: testInfo.outputPath("login.png"), fullPage: true });
  await page.getByRole("textbox", { name: "Username" }).fill(username);
  await page.getByRole("textbox", { name: "Password" }).fill(password);
  await page.getByRole("button", { name: /sign in/i }).click();
  await expect(page).toHaveURL("http://localhost:5173/");
}

test("real operator and auditor sessions enforce role-aware navigation", async ({ browser }, testInfo) => {
  const operatorPassword = process.env.LEDGERFLOW_OPERATOR_PASSWORD;
  const auditorPassword = process.env.LEDGERFLOW_AUDITOR_PASSWORD;
  if (!operatorPassword || !auditorPassword) {
    throw new Error("LEDGERFLOW_OPERATOR_PASSWORD and LEDGERFLOW_AUDITOR_PASSWORD are required");
  }

  const operatorContext = await browser.newContext();
  const operatorPage = await operatorContext.newPage();
  await signIn(operatorPage, "operator", operatorPassword, testInfo);
  await expect(operatorPage.getByText("Operator", { exact: true })).toBeVisible();
  await operatorPage.goto("/accounts");
  await expect(operatorPage.getByRole("link", { name: "Create account" })).toBeVisible();
  await operatorPage.goto("/transfers");
  await expect(operatorPage.getByRole("link", { name: "Create transfer" })).toBeVisible();
  await expect(operatorPage.getByRole("link", { name: "Demo Lab" })).toHaveCount(0);
  await operatorPage.getByRole("button", { name: "Logout" }).click();
  await expect(operatorPage.getByRole("textbox", { name: "Username" })).toBeVisible();
  await operatorContext.close();

  const auditorContext = await browser.newContext();
  const auditorPage = await auditorContext.newPage();
  await signIn(auditorPage, "auditor", auditorPassword, testInfo);
  await expect(auditorPage.getByText("Auditor", { exact: true })).toBeVisible();
  await auditorPage.goto("/accounts");
  await expect(auditorPage.getByRole("link", { name: "Create account" })).toHaveCount(0);
  await auditorPage.goto("/transfers");
  await expect(auditorPage.getByRole("link", { name: "Create transfer" })).toHaveCount(0);
  await expect(auditorPage.getByRole("link", { name: "Demo Lab" })).toHaveCount(0);
  await auditorPage.goto("/demo");
  await expect(auditorPage).toHaveURL("http://localhost:5173/");
  await expect(auditorPage.getByRole("heading", { name: "See the workflow, not a simulation" })).toBeVisible();
  await auditorContext.close();
});

test("real Keycloak, Gateway, Kafka, Redis, and PostgreSQL complete all guided outcomes", async ({ page }, testInfo) => {
  const username = process.env.LEDGERFLOW_ADMIN_USERNAME ?? "admin";
  const password = process.env.LEDGERFLOW_ADMIN_PASSWORD;
  if (!password) throw new Error("LEDGERFLOW_ADMIN_PASSWORD is required for real browser verification");
  const browserErrors: string[] = [];
  page.on("console", (message) => { if (message.type() === "error") browserErrors.push(message.text()); });
  await signIn(page, username, password, testInfo);
  browserErrors.length = 0;
  const authEvidence = await page.evaluate(() => {
    const key = Object.keys(sessionStorage).find((candidate) => candidate.startsWith("oidc.user:"));
    const stored = key ? JSON.parse(sessionStorage.getItem(key) ?? "{}") as {
      access_token?: string;
      profile?: { preferred_username?: string; realm_access?: { roles?: string[] } };
    } : {};
    const payload = stored.access_token?.split(".")[1];
    let tokenClaims: { preferred_username?: string; realm_access?: { roles?: string[] }; aud?: unknown } = {};
    if (payload) {
      const normalized = payload.replaceAll("-", "+").replaceAll("_", "/");
      tokenClaims = JSON.parse(atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "="))) as typeof tokenClaims;
    }
    return {
      profileUsername: stored.profile?.preferred_username,
      profileRoles: stored.profile?.realm_access?.roles ?? [],
      tokenUsername: tokenClaims.preferred_username,
      tokenRoles: tokenClaims.realm_access?.roles ?? [],
      audience: tokenClaims.aud,
    };
  });
  await testInfo.attach("authentication-claims", {
    body: JSON.stringify(authEvidence, null, 2),
    contentType: "application/json",
  });
  console.log(`Authenticated UI evidence: ${JSON.stringify(authEvidence)}`);
  await expect(page.getByRole("heading", { name: "See the workflow, not a simulation" })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("overview.png"), fullPage: true });
  await page.goto("/accounts");
  await expect(page.getByRole("heading", { name: "Accounts" })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("accounts.png"), fullPage: true });
  await page.goto("/transfers");
  await expect(page.getByRole("heading", { name: "Transfers" })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("transfers.png"), fullPage: true });
  await page.goto("/demo");
  await expect(page.getByRole("heading", { name: "Demo Lab" })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("demo-lab.png"), fullPage: true });
  const expectations = [
    { title: "Successful transfer", status: "COMPLETED", source: "EUR 874.50", destination: "EUR 225.50" },
    { title: "Risk rejection + compensation", status: "REJECTED", source: "EUR 1,000.00", destination: "EUR 100.00" },
    { title: "Insufficient funds", status: "REJECTED", source: "EUR 50.00", destination: "EUR 100.00" },
    { title: "Idempotency replay", status: "COMPLETED", source: "EUR 874.50", destination: "EUR 225.50" },
  ] as const;
  const evidence: Record<string, string> = {};
  for (const scenario of expectations) {
    const card = page.getByRole("heading", { name: scenario.title }).locator("..");
    await expect(card.getByRole("button", { name: "Run scenario" })).toBeVisible();
    await card.getByRole("button", { name: "Run scenario" }).click();
    await expect(page.getByRole("heading", { name: "Scenario in progress" })).toBeVisible();
    if (scenario.title === "Successful transfer") {
      await page.screenshot({ path: testInfo.outputPath("workflow-processing.png"), fullPage: true });
    }
    await expect(page.getByRole("heading", { name: "Scenario complete" })).toBeVisible({ timeout: 90_000 });
    await expect(page.getByText(scenario.status, { exact: true }).last()).toBeVisible();
    await expect(page.getByText(scenario.source, { exact: true })).toBeVisible();
    await expect(page.getByText(scenario.destination, { exact: true })).toBeVisible();
    if (scenario.title === "Idempotency replay") {
      await expect(page.getByText("Header confirmed", { exact: true })).toBeVisible();
      await expect(page.getByText("Same ID: true", { exact: true })).toBeVisible();
    }
    const evidencePanel = page.locator(".demo-evidence");
    const transferId = (await evidencePanel.getByRole("link").first().textContent())?.trim() ?? "";
    const correlationId = (await evidencePanel.locator("code").first().textContent())?.trim() ?? "";
    const sourceAccountHref = await evidencePanel.getByRole("link").nth(1).getAttribute("href");
    evidence[scenario.title] = `${transferId}|${correlationId}|${scenario.status}|${scenario.source}|${scenario.destination}`;
    await page.screenshot({ path: testInfo.outputPath(`scenario-${scenario.status.toLowerCase()}-${scenario.title.toLowerCase().replaceAll(/[^a-z]+/g, "-")}.png`), fullPage: true });
    if (scenario.title === "Successful transfer") {
      await evidencePanel.getByRole("link").first().click();
      await expect(page.getByRole("heading", { name: "Immutable state history" })).toBeVisible();
      await page.screenshot({ path: testInfo.outputPath("transfer-completed.png"), fullPage: true });
      await page.goBack();
      if (!sourceAccountHref) throw new Error("Successful scenario did not expose a source account link");
      await page.goto(sourceAccountHref);
      await expect(page.getByRole("heading", { name: "Immutable ledger" })).toBeVisible();
      await page.screenshot({ path: testInfo.outputPath("account-detail.png"), fullPage: true });
      await page.goBack();
    }
    if (scenario.title === "Risk rejection + compensation") {
      await evidencePanel.getByRole("link").first().click();
      await expect(page.getByText("COMPENSATING", { exact: true })).toBeVisible();
      await page.screenshot({ path: testInfo.outputPath("transfer-rejected.png"), fullPage: true });
      await page.goBack();
    }
  }
  await testInfo.attach("scenario-evidence", { body: JSON.stringify(evidence, null, 2), contentType: "application/json" });
  await page.goto("/notifications");
  await expect(page.getByRole("heading", { name: "Terminal notifications" })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("notifications.png"), fullPage: true });
  await page.goto("/system");
  await expect(page.getByRole("heading", { name: "System" })).toBeVisible();
  await expect(page.getByText("HEALTHY", { exact: true })).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("system.png"), fullPage: true });
  await page.setViewportSize({ width: 900, height: 1_000 });
  await page.screenshot({ path: testInfo.outputPath("tablet-system.png"), fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("button", { name: "Open navigation" }).click();
  await expect(page.getByLabel("Application sidebar")).toHaveCSS("transform", "matrix(1, 0, 0, 1, 0, 0)");
  await page.screenshot({ path: testInfo.outputPath("mobile-navigation.png"), fullPage: true });
  expect(browserErrors).toEqual([]);
});
