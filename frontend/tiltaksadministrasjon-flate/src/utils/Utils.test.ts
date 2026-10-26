import { describe, expect, test } from "vitest";
import {
  capitalizeEveryWord,
  getValidationError,
  kalkulerStatusBasertPaaFraOgTilDato,
} from "./Utils";

describe("Utils - getValidationError", () => {
  const errors = [{ pointer: "/tilskudd/0/soknadDato", detail: "Søknadsdato er påkrevd" }];

  test("normalizes validation errors returned at the top level", () => {
    expect(
      getValidationError({
        type: "validation-error",
        title: "Validation error",
        status: 400,
        detail: "Unknown Validation Error",
        errors,
      })?.errors,
    ).toEqual(errors);
  });

  test("normalizes validation errors nested under extensions", () => {
    expect(
      getValidationError({
        type: "validation-error",
        title: "Validation error",
        status: 400,
        detail: "Unknown Validation Error",
        extensions: { errors },
      })?.errors,
    ).toEqual(errors);
  });

  test("does not treat malformed or unrelated errors as validation errors", () => {
    expect(
      getValidationError({ status: 400, errors: [{ message: "Bad request" }] }),
    ).toBeUndefined();
    expect(getValidationError({ status: 500, extensions: { errors } })).toBeUndefined();
  });
});

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
