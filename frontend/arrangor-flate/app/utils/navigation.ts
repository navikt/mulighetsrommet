import { useParams } from "react-router";
import { Tabs } from "~/routes/$orgnr_.oversikt";

export function getCurrentTab(request: Request): Tabs {
  return (new URL(request.url).searchParams.get("forside-tab") as Tabs) || "aktive";
}

export function useOrgnrFromUrl() {
  const { orgnr } = useParams();

  if (!orgnr) {
    throw new Error("Fant ikke orgnr i url");
  }

  return orgnr;
}

export const pathByOrgnr = (orgnr: string) => {
  return {
    utbetalinger: `/${orgnr}/oversikt`,
    opprettKravInnsendingsinformasjon: `/${orgnr}/opprett-krav/innsendingsinformasjon`,
    opprettKravVedlegg: `/${orgnr}/opprett-krav/vedlegg`,
    opprettKravUtbetaling: `/${orgnr}/opprett-krav/utbetaling`,
    opprettKravOppsummering: `/${orgnr}/opprett-krav/oppsummering`,
    innsendingsinformasjon: (id: string) => `/${orgnr}/utbetaling/${id}/innsendingsinformasjon`,
    beregning: (id: string) => `/${orgnr}/utbetaling/${id}/beregning`,
    oppsummering: (id: string) => `/${orgnr}/utbetaling/${id}/oppsummering`,
    kvittering: (id: string) => `/${orgnr}/utbetaling/${id}/kvittering`,
    detaljer: (id: string) => `/${orgnr}/utbetaling/${id}/detaljer`,
    tilsagn: (id: string) => `/${orgnr}/tilsagn/${id}`,
  };
};
