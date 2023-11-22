const base = "arbeidsmarkedstiltak";

export const routes = {
  base,
  detaljer: `${base}/tiltak/:id`,
  detaljer_oppskrift: `oppskrifter/:oppskriftId/:tiltakstypeId`,
  oversikt: `${base}/oversikt`,
} as const;
