import { Ansatt } from "mulighetsrommet-api-client";

export function ansattErTiltaksansvarlig(ansatt?: Ansatt): boolean {
  return !!ansatt?.tilganger?.includes("FLATE");
}
