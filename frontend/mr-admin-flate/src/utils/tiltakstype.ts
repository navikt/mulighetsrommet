import { TiltakstypeDto, TiltakstypeEgenskap } from "@tiltaksadministrasjon/api-client";

export function harEgenskap(tiltakstype: TiltakstypeDto, egenskap: TiltakstypeEgenskap): boolean {
  return tiltakstype.egenskaper.includes(egenskap);
}

export function kanEndreOppstartOgPamelding(tiltakstype: TiltakstypeDto) {
  return harEgenskap(tiltakstype, TiltakstypeEgenskap.STOTTER_FELLES_OPPSTART);
}
