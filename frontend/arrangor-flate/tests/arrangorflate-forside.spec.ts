import { test, expect, Page } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/");
});

const sjekkUU = async (page: Page, waitForTestid: string) => {
  await page.getByTestId(waitForTestid).waitFor();
  const accessibilityScanResults = await new AxeBuilder({ page })
    //.disableRules([])
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
};

test("Kan navigere til forsiden", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle("Arrangørflate");
  await expect(page.getByRole("heading", { name: "Tilgjengelige refusjonskrav" })).toBeVisible();

  await sjekkUU(page, "header");
});
