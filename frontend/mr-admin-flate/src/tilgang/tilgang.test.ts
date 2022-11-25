import { Ansatt } from "mulighetsrommet-api-client";
import { describe, test, expect } from "vitest";
import { ansattErTiltaksansvarlig } from "./tilgang";

describe("Tilgang-tester", () => {
  test("Ansatt med korrekt rolle skal ha tilgang", () => {
    const ansatt: Ansatt = {
      navn: "Tilda",
      tilganger: ["FLATE"],
    };
    expect(ansattErTiltaksansvarlig(ansatt)).toBeTruthy();
  });

  test("Ansatt uten korrekt rolle skal ikke ha tilgang", () => {
    const ansatt: Ansatt = {
      navn: "Fag Ansvarlig",
      tilganger: [],
    };

    expect(ansattErTiltaksansvarlig(ansatt)).toBeFalsy();
  });
});
