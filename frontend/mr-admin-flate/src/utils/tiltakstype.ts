import {
  TiltakstypeEgenskap,
  TiltakstypeFeature,
  TiltakstypeKompaktDto,
} from "@tiltaksadministrasjon/api-client";

export function erUtfaset(tiltakstype: TiltakstypeKompaktDto) {
  return harFeature(tiltakstype, TiltakstypeFeature.UTFASET);
}

export function kanOppretteAvtale(tiltakstype: TiltakstypeKompaktDto) {
  return harEgenskap(tiltakstype, TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE);
}

export function kreverDirekteVedtak(tiltakstype: TiltakstypeKompaktDto) {
  return harEgenskap(tiltakstype, TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK);
}

export function kreverDeltidsprosent(tiltakstype: TiltakstypeKompaktDto) {
  return harEgenskap(tiltakstype, TiltakstypeEgenskap.KREVER_DELTIDSPROSENT);
}

function harEgenskap(tiltakstype: TiltakstypeKompaktDto, egenskap: TiltakstypeEgenskap): boolean {
  return tiltakstype.egenskaper.includes(egenskap);
}

function harFeature(tiltakstype: TiltakstypeKompaktDto, feature: TiltakstypeFeature): boolean {
  return tiltakstype.features.includes(feature);
}
