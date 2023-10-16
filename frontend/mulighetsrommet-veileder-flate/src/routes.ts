const base = "arbeidsmarkedstiltak";

export const routes = {
  base,
  detaljer: `${base}/tiltak/:id`,
  oversikt: `${base}/oversikt`,
} as const;
