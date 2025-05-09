import { expect, test } from "@playwright/test";
import { sjekkUU } from "./utils";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/");
});

test.describe("Smoketest og UU", () => {
  test("Adminflate forside", async ({ page }) => {
    await expect(page).toHaveTitle(/Nav Tiltaksadministrasjon/);
    await sjekkUU(page, "heading");
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
    await expect(page.getByTestId("gjennomforing-tabell")).toBeVisible();
    await sjekkUU(page, "opprett-ny-tiltaksgjenomforing_knapp");
  });

  test("Gjennomføringer", async ({ page }) => {
    await page.getByTestId("forsidekort-gjennomforinger").click();
    await expect(page.getByTestId("header_oversikt-over-gjennomforinger")).toBeVisible();
    await sjekkUU(page, "header_oversikt-over-gjennomforinger");
  });

  test("Gjennomføring - Info", async ({ page }) => {
    await page.getByTestId("forsidekort-gjennomforinger").click();
    await page.getByTestId("filtertab").click();
    await page.getByTestId("gjennomforing-tabell_tittel").first().click();
    await expect(page.getByText("Tiltaksnavn")).toBeVisible();
    await sjekkUU(page, "gjennomforing_info-container");
  });

  test("Notifikasjoner", async ({ page }) => {
    await page.getByTestId("notifikasjoner").click();
    await expect(page.getByTestId("header_oppgaveoversikt")).toBeVisible();
    await sjekkUU(page, "header_oppgaveoversikt");
  });

  test("Arrangører", async ({ page }) => {
    await page.getByRole("button", { name: "Meny" }).click();
    await page.getByRole("link", { name: "Arrangører" }).click();
    await expect(page.getByTestId("header_arrangorer")).toBeVisible();
    await sjekkUU(page, "header_arrangorer");
  });
});
