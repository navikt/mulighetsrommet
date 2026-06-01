import { expect, test } from "@playwright/test";
import { sjekkUU } from "./playwrightUtils";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/arbeidsmarkedstiltak");
});

test.describe("Tiltaksoversikt", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/arbeidsmarkedstiltak");
    const finnNyttArbeidsmarkedstiltakBtn = page.getByTestId("finn-nytt-arbeidsmarkedstiltak-btn");
    await finnNyttArbeidsmarkedstiltakBtn.click();
  });

  test("Sjekk at det er gjennomføringer i oversikten", async ({ page }) => {
    await page.getByLabel(/Liten mulighet til å jobbe/).click();
    await page.getByLabel("Nav Oslo").click();
    const rows = page.getByTestId("oversikt_gjennomforinger").getByRole("link");
    await expect(page.getByTestId("oversikt_gjennomforinger")).toContainText("Avklaring");
    expect(await rows.count()).toBeGreaterThan(5);
  });

  test("Sjekk UU", async ({ page }) => {
    await sjekkUU(page);
  });
});

test.describe("Gjennomføringsdetaljer for alle Nav-ansatte", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/arbeidsmarkedstiltak/oversikt");
    await page.getByLabel(/Liten mulighet til å jobbe/).click();
    await page.getByLabel("Nav Oslo").click();
    await page.getByRole("link", { name: "Sindres mentorordning med Yoda" }).click();
  });

  test("Sjekk riktig gjennomføring", async ({ page }) => {
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

test.describe("Tiltakdetaljer", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/arbeidsmarkedstiltak/oversikt");
    await page.getByRole("link", { name: "Sindres mentorordning med Yoda" }).click();
  });

  test("Sjekk riktig gjennomføring", async ({ page }) => {
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
