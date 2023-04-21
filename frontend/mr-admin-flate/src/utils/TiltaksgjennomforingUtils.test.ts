import { Avtale, NavEnhet } from "mulighetsrommet-api-client";
import { describe, test, expect } from "vitest";
import {
  finnOverordnetEnhetFraAvtale,
  hentEnhetsnavn,
  hentListeMedEnhetsnavn,
} from "./TiltaksgjennomforingUtils";

describe("Tiltaksgjennomforing-utils", () => {
  test("Skal hente navn på enhet gitt en liste med enheter og et enhetsnummer", () => {
    const enheter: NavEnhet[] = [
      {
        navn: "NAV Hvaler",
        enhetNr: "0111",
      },
      {
        navn: "NAV Nordre Follo",
        enhetNr: "0213",
      },
    ];

    const enhetsnummer = "0111";
    const result = hentEnhetsnavn(enheter, enhetsnummer);
    expect(result).toBe("NAV Hvaler");
  });

  test("Skal returnere enhetsnummer dersom enheter er undefined", () => {
    const enheter = undefined;
    const enhetsnummer = "0111";
    const result = hentEnhetsnavn(enheter, enhetsnummer);
    expect(result).toBe("0111");
  });

  test("Skal returnere tom streng dersom enhetsnummer er undefined", () => {
    const enheter: NavEnhet[] = [];
    const enhetsnummer = undefined;
    const result = hentEnhetsnavn(enheter, enhetsnummer);
    expect(result).toBe("");
  });

  test("Skal returnere en sortert (asc) liste med navn gitt en liste med enhetsnumre", () => {
    const enheter: NavEnhet[] = [
      {
        navn: "NAV Nordre Follo",
        enhetNr: "0213",
      },
      {
        navn: "NAV Hvaler",
        enhetNr: "0111",
      },
    ];
    const enhetsnummer = ["0111", "0213"];
    const result = hentListeMedEnhetsnavn(enheter, enhetsnummer);
    expect(result).toStrictEqual(["NAV Hvaler", "NAV Nordre Follo"]);
  });

  test("Skal finne overordnet enhet fra avtalen", () => {
    const avtale: Avtale = {
      avtalenummer: "123",
      avtalestatus: "Aktiv" as any, // Fordi vitest klager ved bruk av enum fra mulighetsrommet-api-client
      avtaletype: "Avtale" as any, // Fordi vitest klager ved bruk av enum fra mulighetsrommet-api-client
      id: "12",
      leverandor: {
        navn: "Joblearn AS",
        organisasjonsnummer: "123456789",
      },
      navEnhet: {
        navn: "Nav Øst-Viken",
        enhetsnummer: "0200",
      },
      navn: "Testavtale",
      prisbetingelser: "",
      startDato: "",
      sluttDato: "",
      tiltakstype: {
        arenaKode: "TIL",
        id: "1",
        navn: "Tiltak",
      },
    };

    const enheter: NavEnhet[] = [
      {
        navn: "NAV Nordre Follo",
        enhetNr: "0213",
        overordnetEnhet: "0200",
      },
      {
        navn: "NAV Hvaler",
        enhetNr: "0111",
      },
      {
        navn: "NAV Øst-Viken",
        enhetNr: "0200",
      },
    ];
    const result = finnOverordnetEnhetFraAvtale(avtale, enheter);
    expect(result?.navn).toBe("NAV Øst-Viken");
  });

  test("Skal returnere undefined dersom vi ikke kjenner til enheten som gjelder for avtalen", () => {
    const avtale: Avtale = {
      avtalenummer: "123",
      avtalestatus: "Aktiv" as any, // Fordi vitest klager ved bruk av enum fra mulighetsrommet-api-client
      avtaletype: "Avtale" as any, // Fordi vitest klager ved bruk av enum fra mulighetsrommet-api-client
      id: "12",
      leverandor: {
        navn: "Joblearn AS",
        organisasjonsnummer: "123456789",
      },
      navEnhet: {
        navn: "Nav Øst-Viken",
        enhetsnummer: "0200",
      },
      navn: "Testavtale",
      prisbetingelser: "",
      startDato: "",
      sluttDato: "",
      tiltakstype: {
        arenaKode: "TIL",
        id: "1",
        navn: "Tiltak",
      },
    };

    const enheter: NavEnhet[] = [
      {
        navn: "NAV Nordre Follo",
        enhetNr: "0213",
        overordnetEnhet: "0200",
      },
      {
        navn: "NAV Hvaler",
        enhetNr: "0111",
      },
    ];
    const result = finnOverordnetEnhetFraAvtale(avtale, enheter);
    expect(result).toBe(undefined);
  });
});
