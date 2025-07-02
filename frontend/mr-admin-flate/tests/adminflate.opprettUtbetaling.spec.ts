import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto("/");
});

const korreksjonsKnappTekst = "Opprett korreksjon på utbetaling"

test.describe("Manuell utbetaling", () => {
  test("Kan navigere til skjema for å opprette utbetalinger", async ({ page }) => {
    await page.getByRole("link", { name: "Gjennomføringer" }).nth(0).click();
    await page.locator("table").getByRole("cell").first().click();
    await page.getByRole("tab", { name: "Utbetalinger" }).click();
    await page.getByRole("button", { name: "Handlinger" }).click();
    await page.getByRole("button", { name: korreksjonsKnappTekst }).click();
    await expect(page).toHaveURL(/.*\/skjema$/);
  });

  test("Skjema for opprett utbetalinger validerer", async ({ page }) => {
    await page.getByRole("link", { name: "Gjennomføringer" }).nth(0).click();
    await page.locator("table").getByRole("cell").first().click();
    await page.getByRole("tab", { name: "Utbetalinger" }).click();
    await page.getByRole("button", { name: "Handlinger" }).click();
    await page.getByRole("button", { name: korreksjonsKnappTekst }).click();
    await page.getByRole("button", { name: "Opprett" }).click();
    await expect(page.getByText("Du må sette startdato for perioden")).toBeVisible();
    await expect(page.getByText("Du må sette sluttdato for perioden")).toBeVisible();
    await expect(page.getByText("Du må skrive inn et beløp")).toBeVisible();
    await expect(page.getByText("Begrunnelsen er for kort (minimum 10 tegn)")).toBeVisible();
  });

  test("Skjema for opprett utbetalinger sender bruker til kostnadsfordeling", async ({ page }) => {
    await page.getByRole("link", { name: "Gjennomføringer" }).nth(0).click();
    await page.locator("table").getByRole("cell").first().click();
    await page.getByRole("tab", { name: "Utbetalinger" }).click();
    await page.getByRole("button", { name: "Handlinger" }).click();
    await page.getByRole("button", { name: korreksjonsKnappTekst }).click();
    await page.getByLabel("Periodestart").fill("01.01.2025");
    await page.getByLabel("Periodeslutt").fill("31.01.2025");
    await page.getByLabel("beløp").fill("100");
    await page
      .getByLabel("Begrunnelse for utbetaling")
      .fill("Må lage en utbetaling pga. investeringstilsagn");
    const kontonummerForArrangor = await page.getByLabel("kontonummer").inputValue();
    await expect(kontonummerForArrangor).toBe("12345678910");
    await expect(
      page.getByText("Dersom kontonummer er feil må arrangør oppdatere kontonummer i Altinn"),
    ).toBeVisible();
    await page.getByRole("button", { name: "Opprett" }).click();
    await expect(page).not.toHaveURL(/.*\/skjema$/); // Sjekker at vi får navigert oss til kostnadsfordeling
  });
});
