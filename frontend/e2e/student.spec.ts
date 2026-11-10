import { expect, test } from "./fixtures";

test.describe("student (demo)", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/student/dashboard");
  });

  test("home shows what's coming up", async ({ page }) => {
    await expect(page.getByRole("heading", { name: "Coming up" })).toBeVisible();
    await expect(page.locator('main a[href^="/student/assignments/"]').first()).toBeVisible();
  });

  test("quiz overview offers a single attempt", async ({ page }) => {
    await page.locator('main a[href^="/student/quizzes/"]').first().click();
    await expect(page).toHaveURL(/\/student\/quizzes\/[^/]+$/);
    await expect(page.getByRole("button", { name: "Start quiz" })).toBeVisible();
  });

  test("a class discussion opens", async ({ page }) => {
    const thread = page.locator('main a[href^="/student/discussions/"]').first();
    await expect(thread).toBeVisible();
    await thread.click();
    await expect(page).toHaveURL(/\/student\/discussions\/[^/]+$/);
    await expect(page.locator("main h1, main h2").first()).toBeVisible();
  });

  test("calendar shows class meetings and due dates", async ({ page }) => {
    await page.goto("/student/calendar");
    await expect(page.getByRole("button", { name: "Class meetings" })).toBeVisible();
    await expect(page.locator('a[href^="/student/assignments/"]', { hasText: "Due:" }).first()).toBeVisible();
  });
});
