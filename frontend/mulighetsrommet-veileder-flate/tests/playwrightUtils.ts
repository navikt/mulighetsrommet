import AxeBuilder from "@axe-core/playwright";
import { Page, expect } from "@playwright/test";

export const sjekkUU = async (page: Page) => {
  const accessibilityScanResults = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .disableRules(["nested-interactive"]) // Disabled pga. helptext i Accordion-header
    .analyze();
  expect(accessibilityScanResults.violations).toEqual([]);
};

export const velgFilter = async (page: Page, filternavn: string) => {
  await page.getByTestId(`filter_radio_${filternavn}`).click();
  await expect(page.getByTestId(`filter_radio_${filternavn}`)).toBeChecked();
  await expect(page.getByTestId(`filtertag_${filternavn}`)).toBeVisible();
};
