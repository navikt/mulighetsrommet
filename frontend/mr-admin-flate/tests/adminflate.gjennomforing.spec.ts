import test, { Page } from "@playwright/test";
import { locateAndFillInput } from "./utils";

const mockAvtaleId = "d1f163b7-1a41-4547-af16-03fd4492b7ba";

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1920, height: 1920 });
  await page.goto(`/avtaler/${mockAvtaleId}`);
});

const fyllInnGjennomforing = async (page: Page) => {
  await page.getByTestId("gjennomforinger-tab").click();
  await page.locator("text=Opprett ny gjennomføring").click();
  await locateAndFillInput(page, "antallPlasser", "20");

  await page.locator('input[name="arrangorId"]').click();
  await page.keyboard.press("ArrowDown");
  await page.keyboard.press("Enter");

  await page.locator('button:has-text("Redaksjonelt innhold")').click();

  await page.fill('textarea[name="beskrivelse"]', "Dette er en test");
  await page.fill('textarea[name="faneinnhold.forHvemInfoboks"]', "Dette er en test");

  await page.click('div[role="textbox"]');
  await page.keyboard.insertText("dette er en test");

  await page.click("input#navRegioner");
  await page.keyboard.press("Enter");

  await page.click("button[type=submit]");

  await page.waitForResponse(
    (response) => response.url().includes("/intern/gjennomforinger") && response.status() === 200,
  );
};

test("Opprett ny gjennomføring", async ({ page }) => {
  await fyllInnGjennomforing(page);
});
