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
    const feilmelding = page.getByTestId("feilmelding-container");
    await expect(feilmelding).toContainText(
      /Du må filtrere på en innsatsgruppe og minst én NAV-enhet for å se tiltaksgjennomføringer/,
    );
    await page.getByLabel("Varig tilpasset innsats").click();
    await page.getByLabel("NAV Oslo").click();
    const rows = page.getByTestId("oversikt_tiltaksgjennomforinger").getByRole("link");
    await expect(page.getByTestId("oversikt_tiltaksgjennomforinger")).toContainText(
      "Avklaring - Fredrikstad",
    );
    expect(await rows.count()).toBeGreaterThan(5);
  });

  test("Sjekk UU", async ({ page }) => {
    await sjekkUU(page);
  });
});

test.describe("Tiltaksgjennomføringsdetaljer for alle NAV-ansatte", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/nav/oversikt");
    await page.getByLabel("Varig tilpasset innsats").click();
    await page.getByLabel("NAV Oslo").click();
    await page.getByRole("link", { name: "Sindres mentorordning med Yoda" }).click();
  });

  test("Sjekk riktig tiltaksgjennomføring", async ({ page }) => {
    const h1 = await page.getByRole("heading", { level: 1 }).innerText();
    expect(h1).toContain("Sindres mentorordning med Yoda");
  });

  test("Sjekk at 'Del med bruker', 'Start påmelding' eller 'Opprett avtale' ikke eksisterer", async ({
    page,
  }) => {
    await expect(page.getByTestId("deleknapp")).toHaveCount(0);
    await expect(page.getByTestId("start-pamelding-lenke")).toHaveCount(0);
    await expect(page.getByTestId("opprettavtaleknapp")).toHaveCount(0);
  });

  test("Sjekk UU", async ({ page }) => {
    await sjekkUU(page);
  });
});
