import { Tiltakskode } from "mulighetsrommet-api-client";

export const TILTAK_MED_FELLES_OPPSTART: Tiltakskode[] = [
  Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
  Tiltakskode.JOBBKLUBB,
  Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
];

export function isTiltakMedFellesOppstart(tiltakskode: Tiltakskode): boolean {
  return TILTAK_MED_FELLES_OPPSTART.includes(tiltakskode);
}

export const ANSKAFFEDE_TILTAK: Tiltakskode[] = [
  Tiltakskode.ARBEIDSRETTET_REHABILITERING,
  Tiltakskode.AVKLARING,
  Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
  Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
  Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
  Tiltakskode.OPPFOLGING,
  Tiltakskode.JOBBKLUBB,
];

export function erAnskaffetTiltak(tiltakskode: Tiltakskode): boolean {
  return ANSKAFFEDE_TILTAK.includes(tiltakskode);
}
