export const internalNavigation = (orgnr: string) => {
  return {
    utbetalinger: `/${orgnr}/oversikt`,
    opprettKravInnsendingsinformasjon: `/${orgnr}/opprett-krav/innsendingsinformasjon`,
    opprettKravVedlegg: `/${orgnr}/opprett-krav/vedlegg`,
    opprettKravUtbetaling: `/${orgnr}/opprett-krav/utbetalingsinformasjon`,
    opprettKravOppsummering: `/${orgnr}/opprett-krav/oppsummering`,
    innsendingsinformasjon: (id: string) => `/${orgnr}/utbetaling/${id}/innsendingsinformasjon`,
    beregning: (id: string) => `/${orgnr}/utbetaling/${id}/beregning`,
    oppsummering: (id: string) => `/${orgnr}/utbetaling/${id}/oppsummering`,
    kvittering: (id: string) => `/${orgnr}/utbetaling/${id}/kvittering`,
    detaljer: (id: string) => `/${orgnr}/utbetaling/${id}/detaljer`,
    tilsagn: (id: string) => `/${orgnr}/tilsagn/${id}`,
  };
};
