import { defineConfig, devices } from "@playwright/test";

/**
 * Browser end-to-end tests (e2e/). They sign in through the landing page's one-click demo, so
 * every run gets its own fresh private classroom and needs no test accounts.
 *
 * Local (default): needs the backend running on :8080 (see .claude/skills/run-locally); the
 * frontend dev server is started here if it isn't already up.
 *   npm run test:e2e
 * Against a deployed site (read-mostly smoke test, only touches the run's own demo classroom):
 *   $env:E2E_BASE_URL = "https://notare-frontend.vercel.app"; npm run test:e2e
 */
const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:3000";
const local = !process.env.E2E_BASE_URL;

export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: true,
  // auth.setup.ts signs in once per role, so a run uses 2 of the 10 demo sign-ins per IP per hour.
  workers: 2,
  retries: local ? 0 : 1,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "setup", testMatch: /auth\.setup\.ts/ },
    {
      name: "teacher",
      testMatch: /teacher\.spec\.ts/,
      dependencies: ["setup"],
      use: { ...devices["Desktop Chrome"], storageState: "e2e/.auth/teacher.json" },
    },
    {
      name: "student",
      testMatch: /student\.spec\.ts/,
      dependencies: ["setup"],
      use: { ...devices["Desktop Chrome"], storageState: "e2e/.auth/student.json" },
    },
  ],
  webServer: local
    ? { command: "npm run dev", url: baseURL, reuseExistingServer: true, timeout: 120_000 }
    : undefined,
});
