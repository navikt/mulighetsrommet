import { describe, expect, test } from "vitest";
import { capitalizeEveryWord, kalkulerStatusBasertPaaFraOgTilDato } from "./Utils";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { formaterDatoSomYYYYMMDD } from "@mr/frontend-common/utils/date";

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

describe("formaterDatoSomYYYYMMDD()", () => {
  const fallback = "<fallbackDate>";

  test("valid dd.MM.yyyy to yyyy-MM-dd", () => {
    expect(formaterDatoSomYYYYMMDD("31.12.2024", fallback)).toBe("2024-12-31");
  });
  test("invalid dd.MM.yyyy to fallback", () => {
    expect(formaterDatoSomYYYYMMDD("31.02.202", fallback)).toBe(fallback);
  });
  test("valid yyyy-MM-dd to yyyy-MM-dd", () => {
    expect(formaterDatoSomYYYYMMDD("2024-12-31", fallback)).toBe("2024-12-31");
  });
  test("invalid yyyy-MM-dd to fallback", () => {
    expect(formaterDatoSomYYYYMMDD("2024-12-3", fallback)).toBe(fallback);
  });
  test("valid date to yyyy-MM-dd", () => {
    expect(formaterDatoSomYYYYMMDD(new Date(2024, 12 - 1, 31), fallback)).toBe("2024-12-31");
  });
  test("invalid datestring to fallback", () => {
    expect(formaterDatoSomYYYYMMDD(new Date("31.12.2024"), fallback)).toBe(fallback);
  });
  test("null to fallback", () => {
    expect(formaterDatoSomYYYYMMDD(null, fallback)).toBe(fallback);
  });
  test("undefined to fallback", () => {
    expect(formaterDatoSomYYYYMMDD(null, fallback)).toBe(fallback);
  });
});
