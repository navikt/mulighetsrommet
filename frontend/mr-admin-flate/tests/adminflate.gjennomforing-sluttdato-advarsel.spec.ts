import { expect, Page, test } from "@playwright/test";

// Gjennomforing med sluttDato = "2029-12-12" og 36 deltakere (fra MSW-mock)
const gjennomforingMedSluttDatoId = "a7d63fb0-4366-412c-84b7-7c15518ee361";

// Gjennomforing med sluttDato = null og 36 deltakere (fra MSW-mock)
const gjennomforingUtenSluttDatoId = "a7d63fb0-4366-412c-84b7-7c15518ee399";

const advarselModalTekst = "Det finnes deltakere påmeldt denne gjennomføringen";

test.describe("Advarsel ved endring av sluttdato", () => {
  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
  });

  test("Viser advarsel når sluttdato var null og settes til en dato", async ({ page }) => {
    await navigerTilRedigerGjennomforing(page, gjennomforingUtenSluttDatoId);

    const opprinneligSluttdato = await getCurrentSluttDato(page);
    expect(opprinneligSluttdato).toBe("");

    await setSluttDato(page, "01.01.2025");

    await expect(page.getByText(advarselModalTekst)).toBeVisible();
  });

  test("Viser advarsel når sluttdato settes til en tidligere dato", async ({ page }) => {
    await navigerTilRedigerGjennomforing(page, gjennomforingMedSluttDatoId);

    const opprinneligSluttdato = await getCurrentSluttDato(page);
    expect(opprinneligSluttdato).toBe("12.12.2029");

    await setSluttDato(page, "01.06.2025");

    await expect(page.getByText(advarselModalTekst)).toBeVisible();
  });

  test("Viser ikke advarsel når sluttdato settes til en senere dato", async ({ page }) => {
    await navigerTilRedigerGjennomforing(page, gjennomforingMedSluttDatoId);

    const opprinneligSluttdato = await getCurrentSluttDato(page);
    expect(opprinneligSluttdato).toBe("12.12.2029");

    await setSluttDato(page, "01.01.2031");

    await expect(page.getByText(advarselModalTekst)).not.toBeVisible();
  });
});

async function navigerTilRedigerGjennomforing(page: Page, gjennomforingId: string) {
  await page.goto(`/gjennomforinger/${gjennomforingId}/rediger`);
  await page.getByLabel("Sluttdato", { exact: true }).waitFor();
}

async function getCurrentSluttDato(page: Page): Promise<string> {
  return await page.getByLabel("Sluttdato", { exact: true }).inputValue();
}

async function setSluttDato(page: Page, dato: string) {
  const input = page.getByLabel("Sluttdato", { exact: true });
  await input.click();
  await input.fill(dato);
  await input.press("Tab");
}
