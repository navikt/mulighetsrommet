import test, { expect, Page } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/avtaler");
});

test("Opprett ny forhåndsgodkjent avtale", async ({ page }) => {
  await fyllInnForhandsgodkjentAvtaleDetaljer(page);
  await fyllInnPrismodell(page);
  await fyllInnPersonopplysninger(page);
  await fyllInnNavRegioner(page);
});

async function fyllInnForhandsgodkjentAvtaleDetaljer(page: Page) {
  await page.locator("text=Opprett ny avtale").click();
  await expect(page.getByText("Opprett ny avtale")).toBeVisible();
  await page.locator('[name="detaljer.sakarkivNummer"]').fill("24/123");
  await page.locator('[name="detaljer.navn"]').fill("Testavtale fra Playwright");
  await page.getByLabel("tiltakstype").selectOption({ value: "ARBEIDSFORBEREDENDE_TRENING" });
  await page.keyboard.press("Enter");
  await page.getByLabel("Startdato").fill("01.02.2025");
  await page.getByRole("button", { name: "Neste" }).click();
}

async function fyllInnPrismodell(page: Page) {
  await page.getByLabel("Prismodell");
  await page.getByRole("button", { name: "Neste" }).click();
}

async function fyllInnPersonopplysninger(page: Page) {
  await page.getByRole("checkbox", { name: "Velg alle" }).check();
  await page.locator("input#bekreft-personopplysninger").check();
  await page.getByRole("button", { name: "Neste" }).click();
}

async function fyllInnNavRegioner(page: Page) {
  await page.locator("text=Opprett avtale").click();
  await expect(page.locator(".aksel-error-summary__item")).toContainText(
    "Du må velge minst én region",
  );

  await page.click("input#navRegioner");
  await page.keyboard.press("Enter");

  await page.click("input#navKontorer");
  await page.keyboard.press("Enter");
  await page.locator("text=Opprett avtale").click();
}
