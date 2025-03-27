export const internalNavigation = (orgnr: string) => {
  return {
    root: `/`,
    utbetalinger: `/${orgnr}/utbetaling`,
    beregning: (id: string) => `/${orgnr}/utbetaling/${id}/beregning`,
    bekreft: (id: string) => `/${orgnr}/utbetaling/${id}/bekreft`,
    innsendtUtbetaling: (id: string) => `/${orgnr}/utbetaling/${id}/innsendt-utbetaling`,
    kvittering: (id: string) => `/${orgnr}/utbetaling/${id}/kvittering`,
    tilsagn: (id: string) => `/${orgnr}/tilsagn/${id}`,
  };
};
