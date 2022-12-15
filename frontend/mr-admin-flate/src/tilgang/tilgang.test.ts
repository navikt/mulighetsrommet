import { Ansatt } from "mulighetsrommet-api-client";
import { describe, test, expect } from "vitest";
import { hentAnsattsRolleForADGruppe } from "./tilgang";

describe("Tilgang-tester", () => {
  test("Ansatt med korrekt rolle skal bli tiltaksansvarlig", () => {
    const ansatt: Ansatt = {
      navn: "Tilda",
      tilganger: ["FLATE"],
    };
    expect(hentAnsattsRolleForADGruppe(ansatt)).toBe("TILTAKSANSVARLIG");
  });

  test("Ansatt med korrekt rolle skal bli fagansvarlig", () => {
    const ansatt: Ansatt = {
      navn: "Frode",
      tilganger: ["FAGANSVARLIG"],
    };
    expect(hentAnsattsRolleForADGruppe(ansatt)).toBe("FAGANSVARLIG");
  });

  test("Ansatt uten korrekt rolle skal ikke ha tilgang", () => {
    const ansatt: Ansatt = {
      navn: "Ikke Ansvarlig",
      tilganger: [],
    };

    expect(hentAnsattsRolleForADGruppe(ansatt)).toBe("UTEN TILGANG");
  });
});
