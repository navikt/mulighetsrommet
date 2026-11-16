import {
  Tiltakskode,
  TiltakstypeEgenskap,
  TiltakstypeFeature,
  TiltakstypeKompaktDto,
} from "@tiltaksadministrasjon/api-client";

const UTGATTE_TILTAKSTYPER_FOR_TILTAKSBESKRIVELSE: Tiltakskode[] = [
  Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
  Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
];

export function erUtfaset(tiltakstype: TiltakstypeKompaktDto) {
  return harFeature(tiltakstype, TiltakstypeFeature.UTFASET);
}

export function kanOppretteTiltakBeskrivelse(tiltakstype: TiltakstypeKompaktDto): boolean {
  return !UTGATTE_TILTAKSTYPER_FOR_TILTAKSBESKRIVELSE.includes(tiltakstype.tiltakskode);
}

export function kreverDirekteVedtak(tiltakstype: TiltakstypeKompaktDto) {
  return harEgenskap(tiltakstype, TiltakstypeEgenskap.KREVER_DIREKTE_VEDTAK);
}

export function kreverDeltidsprosent(tiltakstype: TiltakstypeKompaktDto) {
  return harEgenskap(tiltakstype, TiltakstypeEgenskap.KREVER_DELTIDSPROSENT);
}

export function kreverUtdanningslop(tiltakskode: Tiltakskode): boolean {
  return [Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING, Tiltakskode.FAG_OG_YRKESOPPLAERING].includes(
    tiltakskode,
  );
}

export function kreverAmo(tiltakskode: Tiltakskode): boolean {
  return [
    Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
    Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    Tiltakskode.STUDIESPESIALISERING,
    Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
  ].includes(tiltakskode);
}

function harEgenskap(tiltakstype: TiltakstypeKompaktDto, egenskap: TiltakstypeEgenskap): boolean {
  return tiltakstype.egenskaper.includes(egenskap);
}

function harFeature(tiltakstype: TiltakstypeKompaktDto, feature: TiltakstypeFeature): boolean {
  return tiltakstype.features.includes(feature);
}
