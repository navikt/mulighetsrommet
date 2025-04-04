import { expect, test } from "@playwright/test";
import { sjekkUU } from "./utils";

test.describe("Utbetalinger detaljer", () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the gjennomforinger page
    await page.goto("/gjennomforinger");
    // Wait for the gjennomforinger table to load
    await page.waitForSelector("table");

    // Click on the first gjennomføring's name (tiltaksnavn column)
    const firstGjennomforing = page.locator("tr").nth(1).locator("td").nth(0); // Assuming name is in the second column
    await firstGjennomforing.click();

    // Wait for the gjennomføring details page to load
    await page.waitForSelector("h1");

    // Click on the "Utbetalinger" tab
    await page.locator("button", { hasText: "Utbetalinger" }).click();
  });

  test("should navigate to utbetalinger and select a RETURNERT utbetaling", async ({ page }) => {
    // Wait for the utbetalinger table to load
    await page.waitForSelector("table");

    // Find the row with status RETURNERT
    const returnertUtbetalingRow = page.locator("tr").filter({ hasText: "RETURNERT" }).first();
    await expect(returnertUtbetalingRow).toBeVisible();

    // Click on the "Detaljer" link within that row
    await returnertUtbetalingRow.locator("a", { hasText: "Detaljer" }).click();

    // Verify we're on the utbetaling details page
    await expect(page).toHaveURL(/.*\/utbetalinger\/.*/);
    await expect(page.locator("h2:has-text('Til utbetaling')")).toBeVisible();

    // Verify the status is displayed as RETURNERT
    await expect(page.locator("span", { hasText: "Returnert" })).toBeVisible();

    // Verify the utbetaling linjer are displayed
    await expect(page.locator("a:has-text('A-2025/123')")).toBeVisible();
    await expect(page.locator("table")).toBeVisible();

    // Verify that årsak og forklaring is rendered
    await expect(
      page.locator("h4", { hasText: "Linjen ble returnert på grunn av følgende årsaker" }),
    ).toBeVisible();
    // Verify that the return reason text is visible
    await expect(page.locator("text=Feil beløp")).toBeVisible();
    await expect(
      page.locator("text=Forklaring: Beløpet er feil, og bør fikses ved å endre antall deltakere"),
    ).toBeVisible();
    await sjekkUU(page, "utbetaling-til-utbetaling");
  });
});
