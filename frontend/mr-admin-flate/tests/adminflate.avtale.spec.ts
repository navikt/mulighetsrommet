import test, { expect, Page } from "@playwright/test";
import { locateAndFillComboboxFirst, locateAndFillInput } from "./utils";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/avtaler");
});

const fyllInnAvtale = async (page: Page) => {
  await page.locator("text=Opprett ny avtale").click();
  await expect(page.getByText("Opprett ny avtale")).toBeVisible();
  await locateAndFillInput(page, "navn", "Testavtale fra Playwright");
  await locateAndFillInput(page, "sakarkivNummer", "24/123");
  await locateAndFillComboboxFirst(page, "input#tiltakstype", "AFT");
  await page.fill('.navds-form-field:has(label:text("Startdato")) input', "01.02.2025");
  await locateAndFillComboboxFirst(page, "input#tiltakstype", "AFT");

  await page.getByRole("button", { name: "Neste" }).click();

  await page.getByRole("checkbox", { name: "Velg alle" }).check();
  await page.locator("input#bekreft-personopplysninger").check();

  await page.getByRole("button", { name: "Neste" }).click();
};

test("Opprett ny avtale AFT", async ({ page }) => {
  await fyllInnAvtale(page);

  await page.locator('button:has-text("Opprett avtale")').click();
  const errorMessages = await page.locator(".navds-error-summary__list li").allTextContents();
  expect(errorMessages).toContain("Du må velge minst én region");

  await page.click("input#navRegioner");
  await page.keyboard.press("Enter");

  await page.click("input#navKontorer");
  await page.keyboard.press("Enter");
  await page.locator('button:has-text("Opprett avtale")').click();

  const response = await page.waitForResponse(
    (response) => response.url().includes("/intern/avtaler") && response.status() === 200,
  );

  const { id } = await response.json();
  await page.goto(`/avtaler/${id}`);
});
