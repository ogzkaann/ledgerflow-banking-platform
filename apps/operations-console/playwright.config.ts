import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"], ["html", { outputFolder: "playwright-report", open: "never" }]],
  use: {
    baseURL: "http://localhost:5173",
    actionTimeout: 10_000,
    navigationTimeout: 15_000,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1",
    url: "http://localhost:5173",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: {
      ...process.env,
      VITE_ENABLE_DEMO_FUNDING: "true",
      VITE_ENVIRONMENT: "playwright",
    },
  },
  projects: [
    { name: "mocked", testMatch: /mocked\.spec\.ts/, use: { ...devices["Desktop Chrome"] } },
    { name: "real", testMatch: /real\.spec\.ts/, use: { ...devices["Desktop Chrome"] } },
  ],
});
