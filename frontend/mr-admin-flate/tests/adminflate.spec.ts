import { expect, Page, test } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(async ({ page }) => {
  await page.goto("/");
});

const sjekkUU = async (page: Page) => {
  const accessibilityScanResults = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
};

test.describe("Smoketest og UU", () => {
  test("Adminflate", async ({ page }) => {
    await expect(page).toHaveTitle(/Mulighetsrommet - Admin-flate/);
    await sjekkUU(page);
  });

  test("Tiltakstyper", async ({ page }) => {
    await page.getByTestId("forsidekort-tiltakstyper").click();
    await expect(page.getByTestId("header_oversikt-over-tiltakstyper")).toBeVisible();
    await sjekkUU(page);
  });

  test("Avtaler", async ({ page }) => {
    await page.getByTestId("forsidekort-avtaler").click();
    await expect(page.getByTestId("header_oversikt-over-avtaler")).toBeVisible();
    await sjekkUU(page);
  });

  test("Avtaler - Avtaleinfo Tab", async ({ page }) => {
    await page.getByTestId("forsidekort-avtaler").click();
    await page.getByTestId("filter-tab").click();
    await page.getByTestId("avtaletabell_tittel").first().click();
    await expect(page.getByText("Avtalenavn")).toBeVisible();
    await sjekkUU(page);
  });

  test("Avtaler - Gjennomføringer Tab", async ({ page }) => {
    await page.getByTestId("forsidekort-avtaler").click();
    await page.getByTestId("filter-tab").click();
    await page.getByTestId("avtaletabell_tittel").first().click();
    await page.getByTestId("gjennomforinger-tab").click();
    await expect(page.getByTestId("tiltaksgjennomforing_tabell")).toBeVisible();
    await sjekkUU(page);
  });

  test("Tiltaksgjennomføringer", async ({ page }) => {
    await page.getByTestId("forsidekort-tiltaksgjennomforinger").click();
    await expect(page.getByTestId("header_oversikt-over-tiltaksgjennomforinger")).toBeVisible();
    await sjekkUU(page);
  });

  test("Notifikasjoner", async ({ page }) => {
    await page.getByTestId("notifikasjoner").click();
    await expect(page.getByTestId("header_notifikasjoner")).toBeVisible();
    await sjekkUU(page);
  });
});
