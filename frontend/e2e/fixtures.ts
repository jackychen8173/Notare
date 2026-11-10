import { expect, test as base } from "@playwright/test";

/** Fails the test on any uncaught page error, not just on a failed assertion. */
export const test = base.extend<{ pageErrors: string[] }>({
  pageErrors: [
    async ({ page }, use) => {
      const errors: string[] = [];
      page.on("pageerror", (error) => errors.push(error.message));
      await use(errors);
      expect(errors, "uncaught errors on the page").toEqual([]);
    },
    { auto: true },
  ],
});

export { expect };
