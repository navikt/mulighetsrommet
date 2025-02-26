import { Page } from "@playwright/test";

export async function locateAndFillInput(page: Page, inputName: string, fill: string) {
  const input = page.locator(`input[name=${inputName}]`);
  await input.fill(fill);
}

export async function locateAndFillComboboxFirst(page: Page, inputName: string, value: string) {
  await page.click(inputName);
  await page.fill(inputName, value);
  await page.keyboard.press("Enter");
}
