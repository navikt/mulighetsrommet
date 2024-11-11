import test, { Page } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/avtaler");
});

test("Opprett ny avtale", async ({ page }) => {
  await page.locator("text=Opprett ny avtale").click();
  const heading = page.locator("h2");
  await test.expect(heading).toHaveText("Opprett ny avtale");
  await locateAndFillInput(page, "navn", "Testavtale fra Playwright");
  await locateAndFillInput(page, "websaknummer", "24/123");
  // TODO Flere tester for å fullføre skjema
});

async function locateAndFillInput(page: Page, inputName: string, fill: string) {
  const input = page.locator(`input[name=${inputName}]`);
  await input.fill(fill);
}
