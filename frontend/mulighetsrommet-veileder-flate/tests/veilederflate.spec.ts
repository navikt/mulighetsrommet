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

const velgFilter = async (page: Page, filternavn: string) => {
  await page.getByTestId(`filter_radio_${filternavn}`).click();
  await expect(page.getByTestId(`filter_radio_${filternavn}`)).toBeChecked();
  await expect(page.getByTestId(`filtertag_${filternavn}`)).toBeVisible();
};

test.describe("Tiltaksoversikt", () => {
  test("Sjekk at det er 5 tiltaksgjennomføringer i oversikten", async ({ page }) => {
    const rows = page.getByTestId("oversikt_tiltaksgjennomforinger").getByRole("link");
    await expect(rows).toHaveCount(5);
  });

  test("Sjekk UU", async ({ page }) => {
    await sjekkUU(page);
  });

  test("Filtrer på søkefelt", async ({ page }) => {
    const rows = page.getByTestId("oversikt_tiltaksgjennomforinger").getByRole("link");
    await expect(rows).toHaveCount(5);
    await page.getByTestId("filter_sokefelt").fill("Yoda");
    await expect(rows).toHaveCount(1);
    await expect(
      page.getByTestId("tiltaksgjennomforing_sindres-mentorordning-med-yoda"),
    ).toContainText("Yoda");
  });

  test("Skal vise 'Nullstill filter'-knapp når man filterer på innsatsgruppe", async ({
    page,
  }) => {
    await velgFilter(page, "standard-innsats");
    await expect(page.getByTestId("knapp_nullstill-filter")).toBeVisible();
  });

  test("'Nullstill filter'-knappen fungerer", async ({ page }) => {
    const rows = page.getByTestId("oversikt_tiltaksgjennomforinger").getByRole("link");
    await expect(rows).toHaveCount(5);
    await velgFilter(page, "standard-innsats");
    await expect(page.getByTestId("knapp_nullstill-filter")).toBeVisible();
    await expect(rows).toHaveCount(1);
    await page.getByTestId("knapp_nullstill-filter").click();
    await expect(rows).toHaveCount(5);
  });

  test("Skal vise korrekt feilmelding dersom ingen tiltaksgjennomføringer blir funnet", async ({
    page,
  }) => {
    await page.getByTestId("filter_sokefelt").fill("blablablablabla");
    await expect(page.getByTestId("feilmelding-container")).toBeVisible();
    await expect(page.getByTestId("feilmelding-container")).toHaveAttribute(
      "aria-live",
      "assertive",
    );
  });

  test("Skal åpne historikk-modal", async ({ page }) => {
    await page.getByTestId("historikk_knapp").click();
    await expect(page.getByTestId("historikk_modal")).toBeVisible();
    await expect(page.getByTestId("historikk_modal")).toContainText("Historikk");
  });
});

test.describe("Tiltaksgjennomføringsdetaljer", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/arbeidsmarkedstiltak/oversikt");
    await page.getByTestId("tiltaksgjennomforing_sindres-mentorordning-med-yoda").click();
  });

  test("Sjekk UU", async ({ page }) => {
    await sjekkUU(page);
  });

  test("Sjekk riktig tiltaksgjennomføring", async ({ page }) => {
    await expect(page).toHaveTitle(
      /Arbeidsmarkedstiltak - Detaljer - Sindres mentorordning med Yoda/,
    );
  });

  test("Sjekk at kontaktinfo-fanen viser kontaktinfo", async ({ page }) => {
    await page.getByTestId("fane_kontaktinfo").click();
    await expect(page.getByTestId("fane_panel")).toContainText("Sindre");
  });

  test('Sjekk "Del med bruker"', async ({ page }) => {
    await page.getByTestId("deleknapp").click();
    await expect(page.getByTestId("textarea_deletekst")).toContainText("Hei IHERDIG");
    await expect(page.getByTestId("textarea_deletekst")).toContainText("Jedi Mester");
    await expect(page.getByTestId("textarea_deletekst")).toContainText("Vi holder kontakten!");

    await page.getByTestId("endre-deletekst_btn").click();
    await page.getByTestId("textarea_deletekst").fill("I am your father");

    await expect(page.getByTestId("textarea_deletekst")).not.toContainText("Hei IHERDIG");
    await expect(page.getByTestId("textarea_deletekst")).toContainText("I am your father");

    await page.getByTestId("venter-pa-svar_checkbox").check();
    await page.getByTestId("modal_btn-send").click();

    await expect(page.getByTestId("statusmodal")).toContainText("Tiltaket er delt med brukeren");
  });
});

test.describe("Preview Mulighetsrommet", () => {
  test.beforeEach(async ({ page }) => {
    const url = "/preview/tiltak/f4cea25b-c372-4d4c-8106-535ab10cd586";
    await page.goto(url);
    expect(page.url().includes("/preview/"));
  });

  test("Skal vise en warning på siden om at man er i Preview-modus", async ({ page }) => {
    await expect(page.getByTestId("sanity-preview-alert")).toBeVisible();
  });

  test("Skal kunne åpne del med bruker, men send via Dialog-knapp gir feilmodal", async ({
    page,
  }) => {
    await page.getByTestId("deleknapp").click();
    await expect(page.getByTestId("alert-preview-del-med-bruker")).toContainText(
      "Det er ikke" + " mulig å dele tiltak med bruker i forhåndsvisning",
    );
  });
});
