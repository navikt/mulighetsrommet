import AxeBuilder from "@axe-core/playwright";
import { expect, Locator, Page } from "@playwright/test";

export async function locateAndFillInput(page: Page, inputName: string, fill: string) {
  const input = page.locator(`input[name=${inputName}]`);
  await input.fill(fill);
}

export async function selectFirstComboboxOption(page: Page, locator: string | Locator) {
  const element = typeof locator === "string" ? page.locator(locator) : locator;
  await element.click();
  await page.keyboard.press("ArrowDown");
  await page.keyboard.press("Enter");
  await page.keyboard.press("Escape");
}

export const sjekkUU = async (page: Page, waitForTestid: string) => {
  await page.getByTestId(waitForTestid).waitFor();
  const accessibilityScanResults = await new AxeBuilder({ page })
    .disableRules(["aria-required-children", "definition-list", "dlitem", "svg-img-alt"])
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
    .analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
};
