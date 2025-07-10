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
  test("should navigate to utbetalinger and select a TIL_ATTESTERING utbetaling", async ({
    page,
  }) => {
    // Wait for the utbetalinger table to load
    await expect(page.getByTestId('utbetaling-table')).toBeVisible();

    // Find the row with status Til attestering
    const returnertUtbetalingRow = page
      .getByTestId("utbetaling-table")
      .locator("tr")
      .filter({ hasText: "Til attestering" })
      .first();
    await expect(returnertUtbetalingRow).toBeVisible();

    // Click on the "Behandle" link within that row
    await returnertUtbetalingRow.locator("a", { hasText: "Behandle" }).click();

    // Verify we're on the utbetaling details page

    await expect(page.locator("h2:has-text('Til utbetaling')")).toBeVisible();

    // Verify the utbetaling linjer are displayed
    await expect(page.locator("a:has-text('A-2025/123')")).toBeVisible();
    await expect(page.getByTestId("linje-table")).toBeVisible();

    // Verify that Attester og Send i retur buttons are visible
    await expect(page.locator("tr button", { hasText: /^Attester$/ })).toBeVisible();
    await expect(page.locator("tr button", { hasText: "Send i retur" })).toBeVisible();

    // Verify that Attester opens modal asking for "Attestering av beløp"
    await page.locator("tr button", { hasText: /^Attester$/ }).click();
    await expect(page.locator("h1", { hasText: "Attestere utbetaling" })).toBeVisible();
    await expect(page.locator("button", { hasText: "Avbryt" })).toBeVisible();
    await expect(page.locator("button", { hasText: "Ja, attester beløp" })).toBeVisible();
    await page.locator("dialog button", { hasText: "Avbryt" }).click();

    // Verify that Send i retur opens modal
    await page.locator("tr button", { hasText: "Send i retur" }).click();
    await expect(page.locator("h1", { hasText: "Send i retur med forklaring" })).toBeVisible();
    await expect(page.locator("dialog button", { hasText: "Send i retur" })).toBeVisible();

    await sjekkUU(page, "utbetaling-til-utbetaling");
  });

  test("should navigate to utbetalinger and select a RETURNERT utbetaling", async ({ page }) => {
    // Wait for the utbetalinger table to load
    await expect(page.getByTestId('utbetaling-table')).toBeVisible();

    // Find the row with status RETURNERT
    const returnertUtbetalingRow = page
      .getByTestId("utbetaling-table")
      .locator("tr")
      .filter({ hasText: "RETURNERT" })
      .first();
    await expect(returnertUtbetalingRow).toBeVisible();

    // Click on the "Behandle" link within that row
    await returnertUtbetalingRow.locator("a", { hasText: "Behandle" }).click();

    // Verify we're on the utbetaling details page

    await expect(page.locator("h2:has-text('Til utbetaling')")).toBeVisible();

    // Verify the status is displayed as RETURNERT
    //await expect(page.locator("span", { hasText: "Returnert" })).toBeVisible();

    // Verify the utbetaling linjer are displayed
    await expect(page.locator("a:has-text('A-2025/123')")).toBeVisible();
    await expect(page.getByTestId("linje-table")).toBeVisible();

    // Verify that årsak og forklaring is rendered
    await expect(
      page.locator("h4", { hasText: "Linjen ble returnert på grunn av følgende årsaker" }),
    ).toBeVisible();
    // Verify that the return reason text is visible
    await expect(page.locator("text=Feil beløp")).toBeVisible();
    await expect(
      page.locator("text=Beløpet er feil, og bør fikses ved å endre antall deltakere"),
    ).toBeVisible();
    await sjekkUU(page, "utbetaling-til-utbetaling");
  });

  test("should navigate to utbetalinger and select a OVERFORT_TIL_UTBETALING utbetaling", async ({
    page,
  }) => {
    // Wait for the utbetalinger table to load
    await expect(page.getByTestId('utbetaling-table')).toBeVisible();

    // Find the row with status Overført til utbetaling
    const returnertUtbetalingRow = page
      .getByTestId("utbetaling-table")
      .locator("tr")
      .filter({ hasText: "Overført til utbetaling" })
      .first();
    await expect(returnertUtbetalingRow).toBeVisible();

    // Click on the "Detaljer" link within that row
    await returnertUtbetalingRow.locator("a", { hasText: "Detaljer" }).click();

    // Verify we're on the utbetaling details page

    await expect(page.locator("h2:has-text('Til utbetaling')")).toBeVisible();

    // Verify the utbetaling linjer are displayed
    await expect(page.locator("a:has-text('A-2025/123')")).toBeVisible();
    await expect(page.getByTestId("linje-table")).toBeVisible();

    // Verify the status is displayed as Overført til utbetaling for the utbetalingslinje
    await expect(page.locator("td span", { hasText: "Overført til utbetaling" })).toBeVisible();

    // Verify that the user sees who has behandlet and attestert the utbetaling
    await expect(page.locator("dt", { hasText: "Behandlet av" })).toBeVisible();
    await expect(page.locator("table dd", { hasText: "Bertil Bengtson" })).toBeVisible();
    await expect(page.locator("dt", { hasText: "Attestert av" })).toBeVisible();
    await expect(page.locator("table dd", { hasText: "Per Haraldsen" })).toBeVisible();

    await sjekkUU(page, "utbetaling-til-utbetaling");
  });

  test("should navigate to utbetalinger and select a UTBETALT utbetaling", async ({ page }) => {
    // Wait for the utbetalinger table to load
    await expect(page.getByTestId('utbetaling-table')).toBeVisible();

    // Find the row with status Utbetalt
    const returnertUtbetalingRow = page
      .getByTestId("utbetaling-table")
      .locator("tr")
      .filter({ hasText: "Utbetalt" })
      .first();
    await expect(returnertUtbetalingRow).toBeVisible();

    // Click on the "Detaljer" link within that row
    await returnertUtbetalingRow.locator("a", { hasText: "Detaljer" }).click();

    // Verify we're on the utbetaling details page

    await expect(page.locator("h2:has-text('Til utbetaling')")).toBeVisible();

    // Verify the utbetaling linjer are displayed
    await expect(page.locator("a:has-text('A-2025/123')")).toBeVisible();
    await expect(page.getByTestId("linje-table")).toBeVisible();

    // Verify the status is displayed as Overført til utbetaling for the utbetalingslinje
    await expect(page.locator("td span", { hasText: "Utbetalt" })).toBeVisible();

    // Verify that the user sees who has behandlet and attestert the utbetaling
    await expect(page.locator("dt", { hasText: "Behandlet av" })).toBeVisible();
    await expect(page.locator("table dd", { hasText: "Bertil Bengtson" })).toBeVisible();
    await expect(page.locator("dt", { hasText: "Attestert av" })).toBeVisible();
    await expect(page.locator("table dd", { hasText: "Per Haraldsen" })).toBeVisible();

    await sjekkUU(page, "utbetaling-til-utbetaling");
  });
});
