import { expect, Page, test } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/");
});

const sjekkUU = async (page: Page, waitForTestid: string) => {
  await page.getByTestId(waitForTestid).waitFor();
  const accessibilityScanResults = await new AxeBuilder({ page })
    .disableRules(["aria-required-children", "definition-list", "dlitem", "svg-img-alt"])
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
};

test.describe("Smoketest og UU", () => {
  test("Adminflate forside", async ({ page }) => {
    await expect(page).toHaveTitle(/Nav Tiltaksadministrasjon/);
    await sjekkUU(page, "heading");
  });

  test("Tiltakstyper", async ({ page }) => {
    await page.getByTestId("forsidekort-tiltakstyper").click();
    await expect(page.getByTestId("header_oversikt-over-tiltakstyper")).toBeVisible();
    await sjekkUU(page, "header_oversikt-over-tiltakstyper");
  });

  test("Avtaler", async ({ page }) => {
    await page.getByTestId("forsidekort-avtaler").click();
    await expect(page.getByTestId("header_oversikt-over-avtaler")).toBeVisible();
    await sjekkUU(page, "header_oversikt-over-avtaler");
  });

  test("Avtaler - Info", async ({ page }) => {
    await page.getByTestId("forsidekort-avtaler").click();
    await page.getByTestId("filtertab").click();
    await page.getByTestId("avtaletabell_tittel").first().click();
    await expect(page.getByText("Avtalenavn")).toBeVisible();
    await sjekkUU(page, "avtale_info-container");
  });

  test("Avtaler - Gjennomføringer Tab", async ({ page }) => {
    await page.getByTestId("forsidekort-avtaler").click();
    await page.getByTestId("filtertab").click();
    await page.getByTestId("avtaletabell_tittel").first().click();
    await page.getByTestId("gjennomforinger-tab").click();
    await expect(page.getByTestId("tiltaksgjennomforing-tabell")).toBeVisible();
    await sjekkUU(page, "opprett-ny-tiltaksgjenomforing_knapp");
  });

  test("Tiltaksgjennomføringer", async ({ page }) => {
    await page.getByTestId("forsidekort-tiltaksgjennomforinger").click();
    await expect(page.getByTestId("header_oversikt-over-tiltaksgjennomforinger")).toBeVisible();
    await sjekkUU(page, "header_oversikt-over-tiltaksgjennomforinger");
  });

  test("Tiltaksgjennomføring - Info", async ({ page }) => {
    await page.getByTestId("forsidekort-tiltaksgjennomforinger").click();
    await page.getByTestId("filtertab").click();
    await page.getByTestId("tiltaksgjennomforing-tabell_tittel").first().click();
    await expect(page.getByText("Tiltaksnavn")).toBeVisible();
    await sjekkUU(page, "tiltaksgjennomforing_info-container");
  });

  test("Notifikasjoner", async ({ page }) => {
    await page.getByTestId("notifikasjoner").click();
    await expect(page.getByTestId("header_notifikasjoner")).toBeVisible();
    await sjekkUU(page, "header_notifikasjoner");
  });

  test("Arrangører", async ({ page }) => {
    await page.getByRole("button", { name: "Meny" }).click();
    await page.getByRole("link", { name: "Arrangører" }).click();
    await expect(page.getByTestId("header_arrangorer")).toBeVisible();
    await sjekkUU(page, "header_arrangorer");
  });
});
