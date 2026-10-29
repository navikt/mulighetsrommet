import test, { expect, Page } from "@playwright/test";
import { locateAndFillInput, selectFirstComboboxOption } from "./utils";

const mockAvtaleId = "d1f163b7-1a41-4547-af16-03fd4492b7ba";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto(`/avtaler/${mockAvtaleId}`);
});

const fyllInnGjennomforing = async (page: Page) => {
  await page.getByRole("button", { name: "Handlinger" }).click();
  await page.getByRole("menuitem", { name: "Opprett ny gjennomføring" }).click();

  await locateAndFillInput(page, "antallPlasser", "20");
  await selectFirstComboboxOption(page, page.locator('input[name="arrangorId"]'));
  const ansvarligEnhet = page.getByRole("combobox", { name: "Ansvarlig enhet" });
  await ansvarligEnhet.click();
  const navBjerke = page.getByRole("option", { name: "Nav St. Hanshaugen" });
  await expect(navBjerke).toBeVisible();
  await navBjerke.click();

  await page.locator('button:has-text("Neste")').click();
  await expect(page.locator('textarea[name="veilederinformasjon.beskrivelse"]')).toBeVisible();

  await page.fill('textarea[name="veilederinformasjon.beskrivelse"]', "Dette er en test");
  await page.fill(
    'textarea[name="veilederinformasjon.faneinnhold.forHvemInfoboks"]',
    "Dette er en test",
  );
  await page.click('div[role="textbox"]');
  await page.keyboard.insertText("dette er en test");
  await selectFirstComboboxOption(page, "input#navRegioner");
  await selectFirstComboboxOption(page, "input#navKontorer");

  await page.click("button[type=submit]");

  await page.waitForResponse(
    (response) => response.url().includes("/gjennomforinger") && response.status() === 200,
  );
};

test("Opprett ny gjennomføring", async ({ page }) => {
  await fyllInnGjennomforing(page);
});
