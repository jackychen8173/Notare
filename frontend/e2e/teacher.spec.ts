import { expect, test } from "./fixtures";

test.describe("teacher (demo)", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/dashboard");
  });

  test("home shows the review queue and opens a code review with Sage suggestions", async ({ page }) => {
    await expect(page.getByRole("heading", { name: "To review" })).toBeVisible();
    const queue = page.locator('main a[href^="/submissions/"]');
    await expect(queue.first()).toBeVisible();

    await queue.first().click();
    await expect(page).toHaveURL(/\/submissions\/[^/]+\/review$/);
    await expect(page.getByRole("button", { name: "Release to student" })).toBeVisible();
    // The demo seeds Sage line suggestions; only the teacher sees them, with these actions.
    await expect(page.getByRole("button", { name: "Accept" }).first()).toBeVisible();
    await expect(page.getByRole("button", { name: "Dismiss" }).first()).toBeVisible();
  });

  test("Ctrl+K jumps to a page", async ({ page }) => {
    // Wait for the page to hydrate, or the key press lands before the shortcut listener exists.
    await expect(page.getByRole("heading", { name: "To review" })).toBeVisible();
    const palette = page.getByRole("dialog");
    await expect(async () => {
      await page.keyboard.press("Control+k");
      await expect(palette).toBeVisible({ timeout: 1_000 });
    }).toPass();
    await palette.locator("input").fill("Calendar");
    await expect(palette.getByRole("option", { name: "Calendar" })).toBeVisible();
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/\/calendar$/);
  });

  test("calendar shows class meetings and due dates", async ({ page }) => {
    await page.goto("/calendar");
    await expect(page.getByRole("button", { name: "Class meetings" })).toBeVisible();
    await expect(page.locator('a[href^="/assignments/"]', { hasText: "Due:" }).first()).toBeVisible();
    await expect(page.locator('a[href^="/courses/"]').first()).toBeVisible();
  });

  test("course discussions open with their posts", async ({ page }) => {
    await page.locator('main a[href^="/courses/"]').first().click();
    await page.getByRole("link", { name: /^Discussions/ }).click();
    const thread = page.locator('main a[href^="/discussions/"]').first();
    const title = (await thread.innerText()).split("\n")[0].trim();
    await thread.click();
    await expect(page).toHaveURL(/\/discussions\/[^/]+$/);
    await expect(page.getByRole("heading", { name: title })).toBeVisible();
  });

  test("signing out returns to the public site", async ({ page }) => {
    await page.getByRole("button", { name: "Account menu" }).click();
    await page.getByText("Sign out", { exact: true }).click();
    await expect(page).toHaveURL(/\/(login)?$/);
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login/);
  });
});
