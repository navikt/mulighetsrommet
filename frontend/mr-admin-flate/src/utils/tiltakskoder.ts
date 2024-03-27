import { TiltakskodeArena } from "mulighetsrommet-api-client";

export const TILTAK_MED_FELLES_OPPSTART: TiltakskodeArena[] = [
  TiltakskodeArena.GRUPPEAMO,
  TiltakskodeArena.JOBBK,
  TiltakskodeArena.GRUFAGYRKE,
];

export function isTiltakMedFellesOppstart(tiltakskode: TiltakskodeArena): boolean {
  return TILTAK_MED_FELLES_OPPSTART.includes(tiltakskode);
}

export const ANSKAFFEDE_TILTAK: TiltakskodeArena[] = [
  TiltakskodeArena.ARBRRHDAG,
  TiltakskodeArena.AVKLARAG,
  TiltakskodeArena.DIGIOPPARB,
  TiltakskodeArena.GRUFAGYRKE,
  TiltakskodeArena.GRUPPEAMO,
  TiltakskodeArena.INDOPPFAG,
  TiltakskodeArena.JOBBK,
];

export function erAnskaffetTiltak(tiltakskode: TiltakskodeArena): boolean {
  return ANSKAFFEDE_TILTAK.includes(tiltakskode);
}
