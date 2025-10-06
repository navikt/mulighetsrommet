import { useParams } from "react-router";
import { Tabs } from "~/routes/$orgnr_.oversikt";
import { Environment } from "~/services/environment";

export function getCurrentTab(request: Request): Tabs {
  return new URL(request.url).searchParams.get("forside-tab") as Tabs;
}

export function useOrgnrFromUrl() {
  const { orgnr } = useParams();

  if (!orgnr) {
    throw new Error("Fant ikke orgnr i url");
  }

  return orgnr;
}

export function useGjennomforingIdFromUrl() {
  const { gjennomforingid } = useParams();

  if (!gjennomforingid) {
    throw new Error("Mangler gjennomføring id");
  }

  return gjennomforingid;
}

export const pathByOrgnr = (orgnr: string) => {
  return {
    utbetalinger: `/${orgnr}/oversikt`,
    opprettKravInnsendingsinformasjon: `/${orgnr}/opprett-krav/innsendingsinformasjon`,
    opprettKravVedlegg: `/${orgnr}/opprett-krav/vedlegg`,
    opprettKravUtbetaling: `/${orgnr}/opprett-krav/utbetaling`,
    opprettKravOppsummering: `/${orgnr}/opprett-krav/oppsummering`,
    opprettKrav: {
      tiltaksOversikt: `/${orgnr}/opprett-krav/`,
      investering: {
        innsendingsinformasjon: (gjennomforingId: string) =>
          `/${orgnr}/opprett-krav/${gjennomforingId}/investering/innsendingsinformasjon`,
        utbetaling: (gjennomforingId: string) =>
          `/${orgnr}/opprett-krav/${gjennomforingId}/investering/utbetaling`,
        vedlegg: (gjennomforingId: string) =>
          `/${orgnr}/opprett-krav/${gjennomforingId}/investering/vedlegg`,
        oppsummering: (gjennomforingId: string) =>
          `/${orgnr}/opprett-krav/${gjennomforingId}/investering/oppsummering`,
      },
      driftstilskudd: {
        innsendingsinformasjon: `/${orgnr}/opprett-krav/driftstilskudd/innsendingsinformasjon`,
        utbetaling: `/${orgnr}/opprett-krav/driftstilskudd/utbetaling`,
        oppsummering: `/${orgnr}/opprett-krav/driftstilskudd/oppsummering`,
      },
    },
    innsendingsinformasjon: (id: string) => `/${orgnr}/utbetaling/${id}/innsendingsinformasjon`,
    beregning: (id: string) => `/${orgnr}/utbetaling/${id}/beregning`,
    oppsummering: (id: string) => `/${orgnr}/utbetaling/${id}/oppsummering`,
    kvittering: (id: string) => `/${orgnr}/utbetaling/${id}/kvittering`,
    detaljer: (id: string) => `/${orgnr}/utbetaling/${id}/detaljer`,
    tilsagn: (id: string) => `/${orgnr}/tilsagn/${id}`,
  };
};

export function deltakerOversiktLenke(env: Environment): string {
  if (env === Environment.DevGcp) {
    return "https://amt.intern.dev.nav.no/deltakeroversikt";
  }
  return "https://nav.no/deltakeroversikt";
}
