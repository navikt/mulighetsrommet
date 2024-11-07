import test, { Page } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/avtaler");
});

test("Opprett ny avtale", async ({ page }) => {
  await page.locator("text=Opprett ny avtale").click();
  const heading = page.locator("h2");
  await test.expect(heading).toHaveText("Opprett ny avtale");
  const navnInput = page.locator('input[name="navn"]');
  await navnInput.fill("Testavtale fra Playwright");
  const websaknummerInput = page.locator('input[name="websaknummer"]');
  await websaknummerInput.fill("24/123");
});

async function localeAndFillInput(page: Page, inputName: string, fill: string) {
  const input = page.locator(`input[name=${inputName}]`);
  await input.fill(fill);
}
