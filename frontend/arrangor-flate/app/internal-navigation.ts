export const internalNavigation = (orgnr: string) => {
  return {
    utbetalinger: `/${orgnr}/utbetaling`,
    manueltUtbetalingskrav: `/${orgnr}/manuelt-utbetalingskrav`,
    beregning: (id: string) => `/${orgnr}/utbetaling/${id}/beregning`,
    oppsummering: (id: string) => `/${orgnr}/utbetaling/${id}/oppsummering`,
    kvittering: (id: string) => `/${orgnr}/utbetaling/${id}/kvittering`,
    detaljer: (id: string) => `/${orgnr}/utbetaling/${id}/detaljer`,
    tilsagn: (id: string) => `/${orgnr}/tilsagn/${id}`,
  };
};
