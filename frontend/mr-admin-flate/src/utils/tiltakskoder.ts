import { Tiltakskode } from "@mr/api-client";

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
