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
  await page.fill('.navds-form-field:has(label:text("Startdato")) input', "01.02.2025");
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
  await page.locator('button:has-text("Opprett avtale")').click();
  const errorMessages = await page.locator(".navds-error-summary__list li").allTextContents();
  expect(errorMessages).toContain("Du må velge minst én region");

  await page.click("input#navRegioner");
  await page.keyboard.press("Enter");

  await page.click("input#navKontorer");
  await page.keyboard.press("Enter");
  await page.locator('button:has-text("Opprett avtale")').click();
}
