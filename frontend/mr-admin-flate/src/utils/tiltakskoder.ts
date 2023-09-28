import { Tiltakskode } from "mulighetsrommet-api-client";

export const TILTAK_MED_FELLES_OPPSTART: Tiltakskode[] = [
  Tiltakskode.GRUPPEAMO,
  Tiltakskode.JOBBK,
  Tiltakskode.GRUFAGYRKE,
];

export function isTiltakMedFellesOppstart(tiltakskode: Tiltakskode): boolean {
  return TILTAK_MED_FELLES_OPPSTART.includes(tiltakskode);
}

export const TILTAK_MED_AVTALE_FRA_MULIGHETSROMMET: Tiltakskode[] = [
  Tiltakskode.ARBFORB,
  Tiltakskode.VASV,
];

export function isTiltakMedAvtaleFraMulighetsrommet(tiltakskode: Tiltakskode): boolean {
  return TILTAK_MED_AVTALE_FRA_MULIGHETSROMMET.includes(tiltakskode);
}

export const ANSKAFFEDE_TILTAK: Tiltakskode[] = [
  Tiltakskode.ARBRRHDAG,
  Tiltakskode.AVKLARAG,
  Tiltakskode.DIGIOPPARB,
  Tiltakskode.GRUFAGYRKE,
  Tiltakskode.GRUPPEAMO,
  Tiltakskode.INDOPPFAG,
  Tiltakskode.JOBBK,
];

export function erAnskaffetTiltak(tiltakskode: Tiltakskode): boolean {
  return ANSKAFFEDE_TILTAK.includes(tiltakskode);
}
