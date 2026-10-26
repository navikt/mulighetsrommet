import { Tiltakskode } from "@arbeidsmarkedstiltak/api-client";

const UTFASET_TIL_ERSTATNING: Partial<Record<Tiltakskode, Tiltakskode>> = {
  [Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING]: Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
  [Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING]: Tiltakskode.FAG_OG_YRKESOPPLAERING,
};

export function erSkjultIFilter(tiltakskode: Tiltakskode): boolean {
  return tiltakskode in UTFASET_TIL_ERSTATNING;
}

export function medUtfasedeTiltakskoder(valgte: Tiltakskode[]): Tiltakskode[] {
  const ekstra = Object.entries(UTFASET_TIL_ERSTATNING)
    .filter(([, erstatning]) => valgte.includes(erstatning))
    .map(([utfaset]) => utfaset as Tiltakskode);
  return [...new Set([...valgte, ...ekstra])];
}
