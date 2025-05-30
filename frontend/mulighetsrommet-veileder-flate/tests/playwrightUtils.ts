import AxeBuilder from "@axe-core/playwright";
import type { Innsatsgruppe } from "@mr/api-client-v2";
import { Page, expect } from "@playwright/test";

export async function waitForAppToBeReady(page: Page) {
  // First wait for the web component to be mounted
  await page.waitForSelector("mulighetsrommet-arbeidsmarkedstiltak", { timeout: 60000 });

  // Then wait for the app to be ready inside the shadow DOM
  await page.waitForFunction(
    () => {
      const webComponent = document.querySelector("mulighetsrommet-arbeidsmarkedstiltak");
      if (!webComponent?.shadowRoot) return false;

      // Check if the app is mounted and ready
      const appRoot = webComponent.shadowRoot.querySelector(
        "#mulighetsrommet-arbeidsmarkedstiltak",
      );
      if (!appRoot) return false;

      // Check if there are any loading states
      const loadingElements = webComponent.shadowRoot.querySelectorAll("[data-loading='true']");
      if (loadingElements.length > 0) return false;

      return true;
    },
    { timeout: 60000 },
  );

  // Wait for any network requests to complete
  await page.waitForLoadState("networkidle", { timeout: 60000 });
}

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
