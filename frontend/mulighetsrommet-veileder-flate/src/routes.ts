const base = "arbeidsmarkedstiltak";

export const routes = {
  base,
  detaljer: `${base}/tiltak/:id`,
  detaljer_oppskrift: `oppskrifter/:oppskriftId/:tiltakstypeId`,
  deltaker: `${base}/deltaker`,
  oversikt: `${base}/oversikt`,
} as const;
