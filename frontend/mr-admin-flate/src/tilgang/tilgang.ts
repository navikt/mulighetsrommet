import { Ansatt } from "mulighetsrommet-api-client";

export type Rolle = "TILTAKSANSVARLIG" | "FAGANSVARLIG" | "UTEN TILGANG";

export function hentAnsattsRolle(ansatt?: Ansatt): Rolle {
  if (!!ansatt?.tilganger?.includes("FLATE")) {
    return "TILTAKSANSVARLIG";
  } else if (!!ansatt?.tilganger?.includes("FAGANSVARLIG")) {
    return "FAGANSVARLIG";
  } else {
    return "UTEN TILGANG";
  }
}
