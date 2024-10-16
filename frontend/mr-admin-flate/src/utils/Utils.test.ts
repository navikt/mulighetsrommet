import { AvtaleFilter } from "@/api/atoms";
import { Avtalestatus, Avtaletype, SorteringAvtaler } from "@mr/api-client";
import { describe, expect, test } from "vitest";
import {
  capitalizeEveryWord,
  createQueryParamsForExcelDownloadForAvtale,
  kalkulerStatusBasertPaaFraOgTilDato,
} from "./Utils";

describe("Utils - kalkulerStatusBasertPaaFraOgTilDato", () => {
  test("Skal returnere status 'Aktiv' når nå er større eller lik fra-dato og nå er mindre eller lik til-dato", () => {
    const now = new Date("2023-2-13");
    const fraDato = "2023-1-11";
    const tilDato = "2023-5-17";
    const result = kalkulerStatusBasertPaaFraOgTilDato(
      {
        fraDato,
        tilDato,
      },
      now,
    );
    expect(result).toEqual("Aktiv");
  });
  test("Skal returnere status 'Planlagt' når nå er mindre enn fra-dato", () => {
    const now = new Date("2023-2-13");
    const fraDato = "2023-2-14";
    const tilDato = "2023-5-17";
    const result = kalkulerStatusBasertPaaFraOgTilDato(
      {
        fraDato,
        tilDato,
      },
      now,
    );
    expect(result).toEqual("Planlagt");
  });
  test("Skal returnere status 'Avsluttet' når nå er større enn til-dato", () => {
    const now = new Date("2023-6-6");
    const fraDato = "2023-2-14";
    const tilDato = "2023-5-17";
    const result = kalkulerStatusBasertPaaFraOgTilDato(
      {
        fraDato,
        tilDato,
      },
      now,
    );
    expect(result).toEqual("Avsluttet");
  });
});

describe("Utils - capitalizeEveryWord", () => {
  test("Skal legge på stor forbokstav for alle ord", () => {
    const tekst = "EN TEKST MED STORE BOKSTAVER";
    const result = capitalizeEveryWord(tekst);
    expect(result).toEqual("En Tekst Med Store Bokstaver");
  });
  test("Skal legge på stor forbokstav for alle ord bortsatt fra ignorerte ord", () => {
    const tekst = "EN TEKST MED STORE BOKSTAVER OG noen små bokstaver";
    const result = capitalizeEveryWord(tekst, ["og"]);
    expect(result).toEqual("En Tekst Med Store Bokstaver og Noen Små Bokstaver");
  });
  test("Skal ikke krasje ved tom streng", () => {
    const tekst = "";
    const result = capitalizeEveryWord(tekst);
    expect(result).toEqual("");
  });
});

describe("Avtaletabell", () => {
  test("Skal returnere korrekt searchParams for avtalefilter", () => {
    const filter: AvtaleFilter = {
      sok: "",
      statuser: [Avtalestatus.AKTIV],
      avtaletyper: [Avtaletype.AVTALE],
      navRegioner: ["0600"],
      tiltakstyper: ["123"],
      sortering: {
        sortString: SorteringAvtaler.NAVN_ASCENDING,
        tableSort: {
          orderBy: "navn",
          direction: "ascending",
        },
      },
      arrangorer: ["123456789"],
      visMineAvtaler: true,
      personvernBekreftet: [true],
      page: 0,
      pageSize: 0,
    };

    const queryParams = createQueryParamsForExcelDownloadForAvtale(filter);
    expect(queryParams.get("tiltakstyper")).toEqual("123");
    expect(queryParams.get("statuser")).toEqual("AKTIV");
    expect(queryParams.get("avtaletyper")).toEqual("Avtale");
    expect(queryParams.get("navRegioner")).toEqual("0600");
    expect(queryParams.get("arrangorer")).toEqual("123456789");
    expect(queryParams.get("visMineAvtaler")).toEqual("true");
    expect(queryParams.get("personvernBekreftet")).toEqual("true");
    expect(queryParams.get("size")).toEqual("10000");
    expect(queryParams.get("publisert")).toEqual(null);
  });
});
