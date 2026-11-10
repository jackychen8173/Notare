import { expect, test as setup } from "@playwright/test";

/**
 * One demo sign-in per role per run, saved and reused by every test of that role. The demo
 * endpoint allows 10 sign-ins per IP per hour, so signing in per test would hit the limit.
 */
for (const role of ["teacher", "student"] as const) {
  setup(`sign in as demo ${role}`, async ({ page }) => {
    await page.goto("/");
    await page.getByRole("button", { name: `Try it as a ${role}` }).first().click();
    await page.waitForURL(role === "teacher" ? /\/dashboard$/ : /\/student\/dashboard$/, { timeout: 30_000 });
    await expect(page.getByRole("button", { name: "Account menu" })).toBeVisible();
    await page.context().storageState({ path: `e2e/.auth/${role}.json` });
  });
}
