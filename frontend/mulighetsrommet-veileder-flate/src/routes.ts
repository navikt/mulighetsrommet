export const routes = {
  base: () => 'arbeidsmarkedstiltak',
  detaljer: () => `${routes.base()}/tiltak/:tiltaksnummer`,
  oversikt: () => `${routes.base()}/oversikt`,
};
