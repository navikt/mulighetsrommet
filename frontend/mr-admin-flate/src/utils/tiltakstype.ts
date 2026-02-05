import { TiltakstypeDto, TiltakstypeEgenskap } from "@tiltaksadministrasjon/api-client";

export function kanOppretteAvtale(tiltakstype: TiltakstypeDto) {
  return harEgenskap(tiltakstype, TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE);
}

export function kanEndreOppstartOgPamelding(tiltakstype: TiltakstypeDto) {
  return (
    harEgenskap(tiltakstype, TiltakstypeEgenskap.STOTTER_FELLES_OPPSTART) &&
    harEgenskap(tiltakstype, TiltakstypeEgenskap.STOTTER_LOPENDE_OPPSTART)
  );
}

export function kreverDeltidsprosent(tiltakstype: TiltakstypeDto) {
  return harEgenskap(tiltakstype, TiltakstypeEgenskap.KREVER_DELTIDSPROSENT);
}

function harEgenskap(tiltakstype: TiltakstypeDto, egenskap: TiltakstypeEgenskap): boolean {
  return tiltakstype.egenskaper.includes(egenskap);
}
