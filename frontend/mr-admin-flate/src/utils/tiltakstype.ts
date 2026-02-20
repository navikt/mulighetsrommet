import {
  TiltakstypeDto,
  TiltakstypeEgenskap,
  TiltakstypeFeature,
} from "@tiltaksadministrasjon/api-client";

export function erUtfaset(tiltakstype: TiltakstypeDto) {
  return harFeature(tiltakstype, TiltakstypeFeature.UTFASET);
}

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

function harFeature(tiltakstype: TiltakstypeDto, feature: TiltakstypeFeature): boolean {
  return tiltakstype.features.includes(feature);
}
