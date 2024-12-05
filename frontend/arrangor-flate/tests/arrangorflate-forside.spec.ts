import { test, expect, Page } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/");
});

const sjekkUU = async (page: Page) => {
  const accessibilityScanResults = await new AxeBuilder({ page })
    .disableRules(["svg-img-alt"])
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
};

test("Kan navigere til forsiden", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle("Arrangørflate");
  await expect(page.getByRole("heading", { name: "Tilgjengelige refusjonskrav" })).toBeVisible();

  await sjekkUU(page);
});

test("Kan navigere gjennom hele refusjonskravet", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle("Arrangørflate");
  await expect(page.getByRole("heading", { name: "Tilgjengelige refusjonskrav" })).toBeVisible();
  await sjekkUU(page);
  await page.getByRole("link", { name: "Detaljer" }).first().click();

  await expect(page.getByRole("heading", { name: "Beregning" })).toBeVisible();
  await expect(
    page.getByText(
      "Hvis noen av opplysningene om deltakerne ikke stemmer, må det sendes forslag til Nav om endring via Deltakeroversikten.",
    ),
  ).toBeVisible();
  await sjekkUU(page);
  await page.getByRole("button", { name: "Neste" }).first().click();
  await expect(page.getByRole("heading", { name: "Oppsummering av refusjonskrav" })).toBeVisible();
  await page
    .getByRole("checkbox", {
      name: "Det erklæres herved at alle opplysninger er gitt i henhold til de faktiske forhold",
    })
    .click();
  await sjekkUU(page);
  await page.getByRole("button", { name: "Bekreft og send refusjonskrav" }).first().click();
  await expect(page.getByRole("heading", { name: "Kvittering" })).toBeVisible();

  await sjekkUU(page);
});
