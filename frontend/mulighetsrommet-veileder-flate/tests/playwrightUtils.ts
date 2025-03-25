import AxeBuilder from "@axe-core/playwright";
import type { Innsatsgruppe } from "@mr/api-client-v2";
import { Page, expect } from "@playwright/test";

export async function sjekkUU(page: Page) {
  const accessibilityScanResults = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .disableRules(["nested-interactive"]) // Disabled pga. helptext i Accordion-header
    .analyze();
  expect(accessibilityScanResults.violations).toEqual([]);
}

export async function clickAndExpectInnsatsgruppe(page: Page, innsatsgruppe: Innsatsgruppe) {
  await page.getByTestId(`filter_radio_${innsatsgruppe}`).click();
  await expect(page.getByTestId(`filter_radio_${innsatsgruppe}`)).toBeChecked();
  await expect(page.getByTestId(`filtertag_${innsatsgruppe}`)).toBeVisible();
}
