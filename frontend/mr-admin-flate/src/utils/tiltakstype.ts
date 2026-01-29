import { TiltakstypeDto, TiltakstypeEgenskap } from "@tiltaksadministrasjon/api-client";

export function harEgenskap(tiltakstype: TiltakstypeDto, egenskap: TiltakstypeEgenskap): boolean {
  return tiltakstype.egenskaper.includes(egenskap);
}
