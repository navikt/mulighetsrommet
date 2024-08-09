import { expect, test } from "@playwright/test";
import { sjekkUU, velgFilter } from "./playwrightUtils";

test.beforeEach(async ({ page }) => {
  await page.goto("/");
});

test.describe("Tiltaksoversikt", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/arbeidsmarkedstiltak");
  });

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

  test("Skal vise 'Nullstill filter'-knapp når man filtrerer på innsatsgruppe", async ({
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
    await page.getByRole("link", { name: "Sindres mentorordning med Yoda" }).click();
  });

  test("Sjekk UU", async ({ page }) => {
    await sjekkUU(page);
  });

  test("Sjekk riktig tiltaksgjennomføring", async ({ page }) => {
    const h1 = await page.getByRole("heading", { level: 1 }).innerText();
    expect(h1).toContain("Sindres mentorordning med Yoda");
  });

  test("Sjekk at kontaktinfo-fanen viser kontaktinfo", async ({ page }) => {
    await page.getByRole("tab", { name: "Kontaktinfo" }).click();
    await expect(page.getByTestId("fane_panel")).toContainText("Sindre");
  });

  test('Sjekk "Del med bruker"', async ({ page }) => {
    await page.getByTestId("deleknapp").click();
    await expect(page.getByTestId("textarea_deletekst")).not.toContainText("Hei IHERDIG");
    await expect(page.getByTestId("textarea_deletekst")).toContainText("Jedi Mester");
    await expect(page.getByTestId("textarea_deletekst")).toContainText("Hilsen");

    await page.getByRole("button", { name: "Rediger melding" }).click();
    await page.getByTestId("textarea_deletekst").fill("I am your father");

    await expect(page.getByTestId("textarea_deletekst")).not.toContainText("Hei IHERDIG");
    await expect(page.getByTestId("textarea_deletekst")).toContainText("I am your father");

    await page.getByRole("checkbox", { name: "Venter på svar fra bruker" }).check();
    await page.getByRole("button", { name: "Send via Dialogen" }).click();

    await expect(page.getByTestId("statusmodal")).toContainText("Tiltaket er delt med brukeren");
  });
});

test.describe("Preview Mulighetsrommet", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/preview/tiltak/f4cea25b-c372-4d4c-8106-535ab10cd586");
    expect(page.url().includes("/preview/"));
  });

  test("Skal vise tiltak", async ({ page }) => {
    const h1 = await page.getByRole("heading", { level: 1 }).innerText();
    expect(h1).toEqual("Avklaring - Fredrikstad med ganske langt navn som strekker seg bortover");
  });

  test("Skal vise en warning på siden om at man er i Preview-modus", async ({ page }) => {
    await expect(page.getByText("AdvarselForhåndsvisning av informasjon")).toContainText(
      "Forhåndsvisning av informasjon",
    );
  });

  test("Skal kunne åpne del med bruker, men send via Dialog-knapp gir feilmodal", async ({
    page,
  }) => {
    await page.getByTestId("deleknapp").click();
    await expect(page.getByTestId("alert-preview-del-med-bruker")).toContainText(
      "Det er ikke mulig å dele tiltak med bruker i forhåndsvisning",
    );
  });
});
