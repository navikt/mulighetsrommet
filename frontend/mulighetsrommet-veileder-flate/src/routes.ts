const base = "arbeidsmarkedstiltak";

export const routes = {
  base,
  detaljer: `${base}/tiltak/:id`,
  detaljer_oppskrift: `oppskrifter/:oppskriftId`,
  oversikt: `${base}/oversikt`,
} as const;
