import { AvtaleFilter } from "@/api/atoms";
import { Avtalestatus, Avtaletype, SorteringAvtaler } from "@mr/api-client-v2";
import { describe, expect, test } from "vitest";
import {
  capitalizeEveryWord,
  createQueryParamsForExcelDownloadForAvtale,
  kalkulerStatusBasertPaaFraOgTilDato,
} from "./Utils";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";

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
      personvernBekreftet: true,
      page: 0,
      pageSize: 0,
    };

    const queryParams = createQueryParamsForExcelDownloadForAvtale(filter);
    expect(queryParams.query).toBeDefined();
    expect(queryParams.query!.tiltakstyper).toEqual(["123"]);
    expect(queryParams.query!.statuser).toEqual(["AKTIV"]);
    expect(queryParams.query!.avtaletyper).toEqual(["Avtale"]);
    expect(queryParams.query!.navRegioner).toEqual(["0600"]);
    expect(queryParams.query!.arrangorer).toEqual(["123456789"]);
    expect(queryParams.query!.visMineAvtaler).toEqual(true);
    expect(queryParams.query!.personvernBekreftet).toEqual(true);
    expect(queryParams.query!.size).toEqual(10000);
  });
});

describe("Json pointer", () => {
  test("json pointer konvertering - simple case", () => {
    expect(jsonPointerToFieldPath("/foo/0/bar")).toBe("foo.0.bar");
  });

  test("root path", () => {
    expect(jsonPointerToFieldPath("/")).toBe("");
  });

  test("single level", () => {
    expect(jsonPointerToFieldPath("/foo")).toBe("foo");
  });

  test("numeric key in path", () => {
    expect(jsonPointerToFieldPath("/0/foo")).toBe("0.foo");
  });

  test("nested path with multiple levels", () => {
    expect(jsonPointerToFieldPath("/foo/bar/baz")).toBe("foo.bar.baz");
  });

  test("trailing slash", () => {
    expect(jsonPointerToFieldPath("/foo/bar/")).toBe("foo.bar");
  });

  test("leading and trailing slashes", () => {
    expect(jsonPointerToFieldPath("//foo/bar//")).toBe("foo.bar");
  });

  test("JSON pointer escaping (~0 -> ~, ~1 -> /)", () => {
    expect(jsonPointerToFieldPath("/foo~0bar/baz~1qux")).toBe("foo~bar.baz/qux");
  });

  test("double slashes", () => {
    expect(jsonPointerToFieldPath("/foo//bar")).toBe("foo.bar");
  });

  test("empty input", () => {
    expect(jsonPointerToFieldPath("")).toBe("");
  });

  test("only slash", () => {
    expect(jsonPointerToFieldPath("/")).toBe("");
  });
});
