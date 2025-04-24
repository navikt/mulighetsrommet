export const internalNavigation = (orgnr: string) => {
  return {
    utbetalinger: `/${orgnr}/utbetaling`,
    manueltUtbetalingskrav: `/${orgnr}/manuelt-utbetalingskrav`,
    beregning: (id: string) => `/${orgnr}/utbetaling/${id}/beregning`,
    bekreft: (id: string) => `/${orgnr}/utbetaling/${id}/bekreft`,
    innsendtUtbetaling: (id: string) => `/${orgnr}/utbetaling/${id}/innsendt-utbetaling`,
    detaljer: (id: string) => `/${orgnr}/utbetaling/${id}/detaljer`,
    tilsagn: (id: string) => `/${orgnr}/tilsagn/${id}`,
  };
};
