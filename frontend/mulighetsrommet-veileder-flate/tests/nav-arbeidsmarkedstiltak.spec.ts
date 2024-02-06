import { expect, test } from "@playwright/test";
import { sjekkUU } from "./playwrightUtils";

test.beforeEach(async ({ page }) => {
  await page.goto("/nav");
});

test.describe("Tiltaksoversikt", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/nav/oversikt");
  });

  test("Sjekk at det er tiltaksgjennomføringer i oversikten", async ({ page }) => {
    const rows = page.getByTestId("oversikt_tiltaksgjennomforinger").getByRole("link");
    await expect(page.getByTestId("oversikt_tiltaksgjennomforinger")).toContainText(
      "Avklaring - Fredrikstad",
    );
    await expect(await rows.count()).toBeGreaterThan(5);
  });

  test("Sjekk UU", async ({ page }) => {
    await sjekkUU(page);
  });

  test.describe("Tiltaksgjennomføringsdetaljer for alle NAV-ansatte", () => {
    test.beforeEach(async ({ page }) => {
      await page.goto("/nav/oversikt");
      await page.getByTestId("tiltaksgjennomforing_sindres-mentorordning-med-yoda").click();
    });

    test("Sjekk UU", async ({ page }) => {
      await sjekkUU(page);
    });

    test("Sjekk riktig tiltaksgjennomføring", async ({ page }) => {
      await expect(
        page.getByTestId("tiltaksgjennomforing-header_sindres-mentorordning-med-yoda"),
      ).toContainText("Sindres mentorordning med Yoda");
    });

    test("Sjekk at 'Del me bruker', 'Start påmelding' eller 'Opprett avtale' ikke eksisterer", async ({
      page,
    }) => {
      await expect(page.getByTestId("deleknapp")).toHaveCount(0);
      await expect(page.getByTestId("start-pamelding-lenke")).toHaveCount(0);
      await expect(page.getByTestId("opprettavtaleknapp")).toHaveCount(0);
    });
  });
});
