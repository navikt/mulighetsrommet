const base = "arbeidsmarkedstiltak";

export const routes = {
  base,
  detaljer: `${base}/tiltak/:id`,
  detaljer_oppskrift: `oppskrifter/:oppskriftId/:tiltakstypeId`,
  detaljer_deltaker: `${base}/tiltak/:id/deltaker`,
  oversikt: `${base}/oversikt`,
} as const;
