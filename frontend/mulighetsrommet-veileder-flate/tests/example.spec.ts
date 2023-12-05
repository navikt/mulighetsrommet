import { expect, Page, test } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

// test('has title', async ({ page }) => {
//   await page.goto('https://playwright.dev/');
//
//   // Expect a title "to contain" a substring.
//   await expect(page).toHaveTitle(/Playwright/);
// });
//
// test('get started link', async ({ page }) => {
//   await page.goto('https://playwright.dev/');
//
//   // Click the get started link.
//   await page.getByRole('link', { name: 'Get started' }).click();
//
//   // Expects page to have a heading with the name of Installation.
//   await expect(page.getByRole('heading', { name: 'Installation' })).toBeVisible();
// });

// const { chromium } = require('playwright');

test.beforeEach(async ({ page }) => {
  await page.goto("/");
});

const SjekkUU = async (page: Page) => {
  const accessibilityScanResults = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
};

test.describe("Tiltaksoversikt", () => {
  let antallTiltak;

  test("Sjekk at det er tiltaksgjennomføringer i oversikten", async ({ page }) => {
    expect(page.getByTestId("oversikt_tiltaksgjennomforinger"));
  });

  test("Sjekk UU", async ({ page }) => {
    await SjekkUU(page);
  });

  // it('Lagre antall tiltak uten filtrering', async () => {
  //   const navn = await page.$eval('[data-testid="antall-tiltak"]', (element) => element.textContent);
  //   antallTiltak = parseInt(navn);
  // });
  //
  // it('Filtrer på Tiltakstyper', async () => {
  //   await page.apneLukketFilterAccordion('tiltakstyper', true);
  //   await page.velgFilter('mentor');
  //
  //   const navn = await page.$eval('[data-testid="antall-tiltak"]', (element) => element.textContent);
  //   expect(antallTiltak).not.toEqual(parseInt(navn));
  //
  //   await expect(page).toExist('[data-testid="knapp_tilbakestill-filter"]');
  //   await page.click('[data-testid="knapp_tilbakestill-filter"]');
  //
  //   await expect(page).not.toHaveAttribute('[data-testid="filter_checkbox_avklaring"]', 'checked');
  //   await expect(page).not.toHaveAttribute('[data-testid="filter_checkbox_mentor"]', 'checked');
  //
  //   await page.apneLukketFilterAccordion('tiltakstyper', false);
  // });
  //
  // it('Filtrer på søkefelt', async () => {
  //   await page.type('[data-testid="filter_sokefelt"]', 'Yoda', { delay: 250 });
  //   await expect(page).toContainText('Yoda');
  // });
  //
  // it('Skal vise tilbakestill filter-knapp når filter utenfor normalen hvis brukeren har innsatsgruppe', async () => {
  //   await page.velgFilter('standard-innsats');
  //   await expect(page).toExist('[data-testid="knapp_tilbakestill-filter"]');
  // });
  //
  // it('Skal legge løpende tiltaksgjennomføringer først i rekken ved sortering på oppstartsdato', async () => {
  //   await page.selectOption('[data-testid="sortering-select"]', 'oppstart-ascending');
  //   await expect(page).toContainText('Løpende oppstart');
  // });
  //
  // it('Skal kunne navigere mellom sider via paginering', async () => {
  //   await expect(page).toExist('[data-testid="paginering"]');
  //   await expect(page.locator('[data-testid="paginering"] > *:nth-child(2)')).not.toHaveAttribute('aria-current');
  //   await page.click('[data-testid="paginering"] > *:nth-child(3)');
  //   await expect(page.locator('[data-testid="paginering"] > *:nth-child(3)')).toHaveAttribute('aria-current');
  // });
  //
  // it('Skal ha ferdig utfylt brukers innsatsgruppe hvis bruker har innsatsgruppe', async () => {
  //   await page.resetSide();
  //   await expect(page).toHaveAttribute('[data-testid="filter_checkbox_situasjonsbestemt-innsats"]', 'checked');
  //   await expect(page).not.toExist('[data-testid="knapp_tilbakestill-filter"]');
  //   await expect(page).toContainText('[data-testid="filtertag_situasjonsbestemt-innsats"]', 'Situasjonsbestemt innsats');
  // });
  //
  // it('Skal huske filtervalg mellom detaljvisning og listevisning', async () => {
  //   await page.click('[data-testid="filter_checkbox_standard-innsats"]');
  //   await page.forventetAntallFiltertags(2);
  //   await page.click('[data-testid="filter_checkbox_situasjonsbestemt-innsats"]');
  //
  //   await page.click('[data-testid="lenke_tiltaksgjennomforing"]');
  //   await page.tilbakeTilListevisning();
  //   await expect(page).toHaveAttribute('[data-testid="filter_checkbox_situasjonsbestemt-innsats"]', 'checked');
  //   await page.forventetAntallFiltertags(2);
  // });
  //
  // it('Skal vise korrekt feilmelding dersom ingen tiltaksgjennomføringer blir funnet', async () => {
  //   await page.type('[data-testid="filter_sokefelt"]', 'blablablablabla', { delay: 250 });
  //   await expect(page).toBeVisible('[data-testid="feilmelding-container"]');
  //   await expect(page).toHaveAttribute('[data-testid="feilmelding-container"]', 'aria-live');
  //   await expect(page).toExist('[data-testid="knapp_tilbakestill-filter"]');
  //   await page.click('[data-testid="knapp_tilbakestill-filter"]');
  // });
});

// xdescribe("Tiltaksgjennomføringsdetaljer", ({ page }) => {
//   beforeEach(async () => {
//     await page.context().clearCookies();
//     await page.goto("/arbeidsmarkedstiltak/oversikt");
//     await page.click('[data-testid="lenke_tiltaksgjennomforing"]');
//   });
//
//   it("Gå til en tiltaksgjennomføring", async () => {
//     await page.checkAccessibility();
//   });
//
//   it("Sjekk at fanene fungerer som de skal", async () => {
//     await expect(page).toBeVisible('[data-testid="tab1"]');
//     await expect(page).not.toBeVisible('[data-testid="tab2"]');
//
//     await page.click('[data-testid="fane_detaljer-og-innhold"]');
//
//     await expect(page).not.toBeVisible('[data-testid="tab1"]');
//     await expect(page).toBeVisible('[data-testid="tab2"]');
//   });
//
//   it('Sjekk "Del med bruker"', async () => {
//     await expect(page).toBeVisible('[data-testid="deleknapp"]');
//     await page.click('[data-testid="deleknapp"]');
//
//     await expect(page).toBeVisible('[data-testid="modal_header"]');
//     await page.click('[data-testid="personlig_intro_btn"]');
//     await page.type('[data-testid="textarea_intro"]', "En spennende tekst", { delay: 250 });
//     await expect(page).not.toExist(".navds-error-message");
//
//     await page.click('[data-testid="personlig_hilsen_btn"]');
//     await page.type('[data-testid="textarea_hilsen"]', "Test", { delay: 250 });
//   });
// });
