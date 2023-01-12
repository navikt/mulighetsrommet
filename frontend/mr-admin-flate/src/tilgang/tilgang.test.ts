import { Ansatt } from "mulighetsrommet-api-client";
import { describe, test, expect } from "vitest";
import { hentAnsattsRolle } from "./tilgang";

describe("Tilgang-tester", () => {
  const baseAnsatt: Ansatt = {
    etternavn: null,
    fornavn: null,
    navn: null,
    ident: "ABC123",
    tilganger: [],
    hovedenhet: "2990",
    hovedenhetNavn: "Østfold",
  };
  test("Ansatt med korrekt rolle skal bli tiltaksansvarlig", () => {
    const ansatt: Ansatt = {
      ...baseAnsatt,
      navn: "Tilda",
      tilganger: ["FLATE"],
    };
    expect(hentAnsattsRolle(ansatt)).toBe("TILTAKSANSVARLIG");
  });

  test("Ansatt med korrekt rolle skal bli fagansvarlig", () => {
    const ansatt: Ansatt = {
      ...baseAnsatt,
      navn: "Frode",
      tilganger: ["FAGANSVARLIG"],
    };
    expect(hentAnsattsRolle(ansatt)).toBe("FAGANSVARLIG");
  });

  test("Ansatt uten korrekt rolle skal ikke ha tilgang", () => {
    const ansatt: Ansatt = {
      ...baseAnsatt,
      navn: "Ikke Ansvarlig",
      tilganger: [],
    };

    expect(hentAnsattsRolle(ansatt)).toBe("UTEN TILGANG");
  });

  test("Medlem av Team Valp skal få rollen utvikler", () => {
    const ansatt: Ansatt = {
      ...baseAnsatt,
      navn: "Valp Valpesen",
      tilganger: ["UTVIKLER_VALP"],
    };
    expect(hentAnsattsRolle(ansatt)).toBe("UTVIKLER");
  });
});
