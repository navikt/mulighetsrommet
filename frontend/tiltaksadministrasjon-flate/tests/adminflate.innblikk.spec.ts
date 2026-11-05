import { expect, test, type Page } from "@playwright/test";

const DEV_SCRIPT = "https://cdn.nav.no/team-researchops/sporing/sporing-dev.js";
const EVENT_PROXY = "https://reops-event-proxy.ekstern.dev.nav.no/api/send";

async function collectFilterEvents(page: Page) {
  const events: Record<string, unknown>[] = [];
  await page.exposeFunction("recordFilterEvent", (data: Record<string, unknown>) => {
    events.push(data);
  });
  await page.context().route(DEV_SCRIPT, (route) =>
    route.fulfill({
      contentType: "application/javascript",
      body: `window.umami = {
        track: (name, data) => {
          if (name === "FILTER_ENDRET") window.recordFilterEvent(data);
        }
      };`,
    }),
  );
  return events;
}

test("sporer enkeltvalg, massevalg og fjerning av chips", async ({ page }) => {
  const events = await collectFilterEvents(page);
  await page.addInitScript(() => {
    const values = {
      statuser: [],
      kostnadssteder: ["0313"],
      sortering: {
        sortString: "navn-ascending",
        tableSort: { orderBy: "navn", direction: "ascending" },
      },
      page: 1,
      pageSize: 15,
    };
    sessionStorage.setItem(
      "Tilsagn-filter-a7d63fb0-4366-412c-84b7-7c15518ee361",
      JSON.stringify({
        filter: { values },
        defaultFilter: { values: { ...values, kostnadssteder: [] } },
        isInitialized: true,
      }),
    );
  });
  await page.goto("/gjennomforinger/a7d63fb0-4366-412c-84b7-7c15518ee361/tilsagn");
  await page.getByRole("button", { name: "Filter", exact: true }).click();
  const status = page.getByRole("checkbox", { name: "Til godkjenning", exact: true });
  const selectAll = page.getByRole("checkbox", { name: "Velg alle", exact: true });

  await status.click();
  await expect(status).toBeChecked();
  await expect
    .poll(() => events)
    .toEqual([
      {
        appnavn: "tiltaksadministrasjon-flate",
        handling: "valgt",
        kilde: "enkeltvalg",
        kategori: "Status",
        filternavn: "TIL_GODKJENNING",
        kontekst: "tilsagn",
      },
    ]);
  await selectAll.click();
  await expect(selectAll).toBeChecked();
  await expect.poll(() => events.length).toBe(7);
  expect(events.slice(1).map(({ filternavn }) => filternavn)).toEqual([
    "GODKJENT",
    "RETURNERT",
    "TIL_ANNULLERING",
    "ANNULLERT",
    "TIL_OPPGJOR",
    "OPPGJORT",
  ]);
  for (const event of events.slice(1)) {
    expect(event).toMatchObject({ handling: "valgt", kilde: "velg_alle" });
  }

  await page.getByRole("button", { name: "Filter", exact: true }).click();
  await page
    .getByTestId("filtertags")
    .getByRole("button", { name: /Til godkjenning/ })
    .click();
  await expect.poll(() => events.length).toBe(8);
  expect(events[7]).toMatchObject({
    filternavn: "TIL_GODKJENNING",
    handling: "fjernet",
    kilde: "enkeltvalg",
  });
  await page.getByRole("button", { name: "Filter", exact: true }).click();
  await expect(status).not.toBeChecked();
  await selectAll.click();
  await expect(selectAll).not.toBeChecked();
  await expect.poll(() => events.length).toBe(14);
  for (const event of events.slice(8)) {
    expect(event).toMatchObject({ handling: "fjernet", kilde: "fjern_alle" });
  }

  await page.getByRole("button", { name: "Filter", exact: true }).click();
  const kostnadsstedChip = page
    .getByTestId("filtertags")
    .getByRole("button", { name: /Nav St\. Hanshaugen/ });
  await kostnadsstedChip.click();
  await expect(kostnadsstedChip).not.toBeVisible();
  await expect.poll(() => events.length).toBe(15);
  expect(events[14]).toEqual({
    appnavn: "tiltaksadministrasjon-flate",
    handling: "fjernet",
    kilde: "fjern_alle",
    kategori: "Kostnadssted",
    filternavn: "Nav Oslo",
    subFilter: "Nav St. Hanshaugen",
    kontekst: "tilsagn",
  });
});

test("nullstilling logger fjerning av valgte statuser og kostnadssteder", async ({ page }) => {
  const events = await collectFilterEvents(page);
  await page.addInitScript(() => {
    const values = {
      statuser: ["TIL_GODKJENNING"],
      kostnadssteder: ["0313"],
      sortering: {
        sortString: "navn-ascending",
        tableSort: { orderBy: "navn", direction: "ascending" },
      },
      page: 1,
      pageSize: 15,
    };
    sessionStorage.setItem(
      "Tilsagn-filter-a7d63fb0-4366-412c-84b7-7c15518ee361",
      JSON.stringify({
        filter: { values },
        defaultFilter: { values: { ...values, statuser: [], kostnadssteder: [] } },
        isInitialized: true,
      }),
    );
  });
  await page.goto("/gjennomforinger/a7d63fb0-4366-412c-84b7-7c15518ee361/tilsagn");
  const resetButton = page.getByRole("button", { name: "Nullstill filter", exact: true });
  await resetButton.click();
  await expect(resetButton).not.toBeVisible();
  await expect
    .poll(() => events)
    .toEqual([
      {
        appnavn: "tiltaksadministrasjon-flate",
        kategori: "Status",
        filternavn: "TIL_GODKJENNING",
        handling: "fjernet",
        kilde: "nullstill",
        kontekst: "tilsagn",
      },
      {
        appnavn: "tiltaksadministrasjon-flate",
        kategori: "Kostnadssted",
        filternavn: "Nav Oslo",
        subFilter: "Nav St. Hanshaugen",
        handling: "fjernet",
        kilde: "nullstill",
        kontekst: "tilsagn",
      },
    ]);
});

test("laster CDN-skriptet for utvikling og sender filtrerte hendelser", async ({ page }) => {
  await page.context().route(EVENT_PROXY, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      headers: {
        "access-control-allow-origin": "*",
        "access-control-allow-headers": "*",
        "access-control-allow-methods": "POST, OPTIONS",
      },
      body: "{}",
    });
  });

  await page.context().route(DEV_SCRIPT, async (route) => {
    const response = await route.fetch();
    expect(response.ok()).toBe(true);
    const source = await response.text();
    // Tillat den lokale hosten uten å endre sporingslogikken i CDN-skriptet.
    await route.fulfill({
      response,
      body: `document.currentScript.setAttribute("data-domains", location.hostname);\n${source}`,
    });
  });

  const pageViewRequest = page.waitForRequest(
    (request) => request.url() === EVENT_PROXY && request.method() === "POST",
  );
  await page.goto("/?cdn-test=query-marker#hash-marker");
  const origin = new URL(page.url()).origin;
  const pageView = await pageViewRequest;

  expect(pageView.postDataJSON()).toMatchObject({
    type: "event",
    payload: { url: `${origin}/`, title: "/" },
  });

  const eventRequest = page.waitForRequest(
    (request) =>
      request.url() === EVENT_PROXY &&
      request.method() === "POST" &&
      request.postDataJSON().payload?.name === "FILTER_ENDRET",
  );
  await page.evaluate(() => {
    window.umami?.track("FILTER_ENDRET", {
      appnavn: "tiltaksadministrasjon-flate",
      handling: "valgt",
      kilde: "velg_alle",
      kategori: "Tiltakstype",
      filternavn: "Arbeidsforberedende trening",
      kontekst: "avtaler",
    });
  });
  const event = await eventRequest;

  expect(event.postDataJSON()).toMatchObject({
    type: "event",
    payload: {
      url: `${origin}/`,
      title: "/",
      name: "FILTER_ENDRET",
      data: {
        appnavn: "tiltaksadministrasjon-flate",
        handling: "valgt",
        kilde: "velg_alle",
        kategori: "Tiltakstype",
        filternavn: "Arbeidsforberedende trening",
        kontekst: "avtaler",
      },
    },
  });
});
