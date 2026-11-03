import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { RouteObject } from "react-router";
import { type InnblikkBeforeSend, createInnblikkBeforeSend, initializeInnblikk } from "./innblikk";

const origin = "https://admin.example.test";
const routes: RouteObject[] = [
  {
    path: "/",
    children: [
      { index: true },
      { path: "avtaler/:avtaleId" },
      { path: "gjennomforinger/:gjennomforingId/deltakerliste/*" },
    ],
  },
  { path: "/absolutt/:ressursId" },
];

function pageView(url: string, extra: Record<string, unknown> = {}) {
  return {
    website: "website-id",
    hostname: "untrusted.example",
    screen: "1920x1080",
    language: "nb-NO",
    url,
    title: "dynamisk-tittel",
    referrer: `${origin}/avtaler/avtale-1?query=hemmelig#hash`,
    ...extra,
  };
}

const statusData = {
  appnavn: "tiltaksadministrasjon-flate",
  handling: "valgt",
  kilde: "enkeltvalg",
  kategori: "Status",
  filternavn: "TIL_GODKJENNING",
  kontekst: "tilsagn",
};
const customEvent = pageView(`${origin}/avtaler/avtale-1?secret=yes`, {
  name: "FILTER_ENDRET",
  data: statusData,
  tag: undefined,
  id: undefined,
});

describe("createInnblikkBeforeSend", () => {
  const filter = createInnblikkBeforeSend(routes, origin);

  it.each([
    ["/avtaler/avtale-1?query=hemmelig#hash", "/avtaler/:avtaleId"],
    [
      "/gjennomforinger/gjennomforing-123/deltakerliste/person-456",
      "/gjennomforinger/:gjennomforingId/deltakerliste/*",
    ],
    ["/absolutt/ressurs-789?secret=value#fragment", "/absolutt/:ressursId"],
  ])("normaliserer %s til %s og fjerner ukjente felter", (path, pattern) => {
    expect(
      filter(
        "event",
        pageView(`${origin}${path}`, { tag: "private-tag", unexpected: "private-value" }),
      ),
    ).toEqual({
      website: "website-id",
      hostname: "admin.example.test",
      screen: "1920x1080",
      language: "nb-NO",
      url: `${origin}${pattern}`,
      title: pattern,
      referrer: `${origin}/avtaler/:avtaleId`,
    });
  });

  it.each([
    `${origin}/ukjent?secret=yes`,
    "https://external.example.test/avtaler/avtale-1",
    "http://[invalid-url",
    `${origin}/avtaler/invalid%`,
  ])("avviser URL-en %s", (url) => {
    expect(filter("event", pageView(url))).toBe(false);
  });

  it("utelater en ekstern referrer uten å avvise sidevisningen", () => {
    expect(
      filter(
        "event",
        pageView(`${origin}/avtaler/avtale-1`, {
          referrer: "https://external.example.test/avtaler/avtale-2",
        }),
      ),
    ).toMatchObject({
      url: `${origin}/avtaler/:avtaleId`,
      referrer: undefined,
    });
  });

  it.each([
    { name: "FILTER_ENDRET", data: statusData },
    {
      name: "FILTER_ENDRET",
      data: {
        appnavn: statusData.appnavn,
        handling: "fjernet",
        kilde: "fjern_alle",
        kategori: "Tiltakstype",
        filternavn: "Arbeidsforberedende trening",
        subFilter: "Gruppe",
        kontekst: "avtaler",
      },
    },
    {
      name: "ACCORDION_ÅPNET",
      data: { appnavn: statusData.appnavn, tittel: "Tiltakstype", kontekst: "avtaler" },
    },
    {
      name: "ACCORDION_LUKKET",
      data: { appnavn: statusData.appnavn, tittel: "Arrangør", kontekst: "gjennomforinger" },
    },
  ])("tillater $name med data $data", ({ name, data }) => {
    expect(filter("event", { ...customEvent, name, data })).toEqual({
      website: "website-id",
      hostname: "admin.example.test",
      screen: "1920x1080",
      language: "nb-NO",
      url: `${origin}/avtaler/:avtaleId`,
      title: "/avtaler/:avtaleId",
      referrer: `${origin}/avtaler/:avtaleId`,
      name,
      data,
    });
  });

  it.each<[string, Record<string, unknown>]>([
    ["ekstra datafelt", { data: { ...statusData, status: "secret" } }],
    ["manglende filternavn", { data: { ...statusData, filternavn: undefined } }],
    ["ekstra metadata", { unexpected: "private-value" }],
    ["tag", { tag: "private-tag" }],
    ["identifikator", { id: "private-id" }],
    ["ukjent hendelsesnavn", { name: "unapproved" }],
    ["prototypemetode", { name: "toString" }],
  ])("avviser hendelser med %s", (_, extra) => {
    expect(filter("event", { ...customEvent, ...extra })).toBe(false);
  });

  it("avviser identifikasjon og data uten hendelsesnavn", () => {
    expect(filter("identify", customEvent)).toBe(false);
    expect(filter("event", pageView(`${origin}/`, { data: { private: "value" } }))).toBe(false);
  });
});

describe("initializeInnblikk", () => {
  let beforeSend: InnblikkBeforeSend;
  const payload = pageView(`${origin}/stale-script-cache`);

  beforeEach(() => {
    window.history.replaceState(null, "", "/");
    delete window.umami;
    initializeInnblikk(routes, window);
    if (!window.innblikkBeforeSend) {
      throw new Error("Før-send-filteret for Innblikk er ikke initialisert");
    }
    beforeSend = window.innblikkBeforeSend;
  });

  afterEach(() => {
    window.history.replaceState(null, "", "/");
    delete window.innblikkBeforeSend;
    delete window.umami;
  });

  it("bruker gjeldende URL ved lasting, pushState og replaceState", () => {
    expect(beforeSend("event", payload)).toMatchObject({
      url: `${window.location.origin}/`,
      title: "/",
    });

    window.history.pushState(null, "", "/avtaler/avtale-1?query=first#first");
    expect(beforeSend("event", payload)).toMatchObject({
      url: `${window.location.origin}/avtaler/:avtaleId`,
    });

    window.history.pushState(null, "", "/avtaler/avtale-1?query=second#second");
    expect(beforeSend("event", payload)).toBe(false);

    window.history.replaceState(
      null,
      "",
      "/gjennomforinger/gjennomforing-2/deltakerliste/person-2",
    );
    expect(beforeSend("event", payload)).toMatchObject({
      url: `${window.location.origin}/gjennomforinger/:gjennomforingId/deltakerliste/*`,
    });
    expect(beforeSend("event", payload)).toBe(false);
  });

  it("registrerer sidevisningen når brukeren navigerer tilbake", async () => {
    beforeSend("event", payload);
    window.history.pushState(null, "", "/avtaler/avtale-1");
    beforeSend("event", payload);

    const backNavigation = new Promise<void>((resolve) => {
      window.addEventListener("popstate", () => resolve(), { once: true });
    });
    window.history.back();
    await backNavigation;

    expect(window.location.pathname).toBe("/");
    expect(beforeSend("event", payload)).toMatchObject({
      url: `${window.location.origin}/`,
      referrer: `${window.location.origin}/avtaler/:avtaleId`,
    });
  });

  it("tillater gjentatte hendelser uten å endre sidevisningstilstanden", () => {
    expect(beforeSend("event", payload)).toMatchObject({
      url: `${window.location.origin}/`,
    });
    const firstCustomEvent = beforeSend("event", customEvent);
    expect(firstCustomEvent).toMatchObject({
      name: "FILTER_ENDRET",
      url: `${window.location.origin}/`,
      referrer: `${window.location.origin}/`,
    });
    expect(beforeSend("event", customEvent)).toEqual(firstCustomEvent);
    expect(beforeSend("event", payload)).toBe(false);

    window.history.pushState(null, "", "/avtaler/avtale-1");
    const customEventOnNewPage = beforeSend("event", customEvent);
    expect(customEventOnNewPage).toMatchObject({
      name: "FILTER_ENDRET",
      url: `${window.location.origin}/avtaler/:avtaleId`,
      referrer: `${window.location.origin}/`,
    });
    expect(beforeSend("event", payload)).toMatchObject({
      url: `${window.location.origin}/avtaler/:avtaleId`,
      referrer: `${window.location.origin}/`,
    });
    expect(beforeSend("event", payload)).toBe(false);
  });
});
