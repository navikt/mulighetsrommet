const base = "arbeidsmarkedstiltak";

export const routes = {
  base,
  detaljer: `${base}/tiltak/:tiltaksnummer`,
  oversikt: `${base}/oversikt`,
} as const;
