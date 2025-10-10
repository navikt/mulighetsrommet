import { OpprettKravVeiviserSteg } from "@api-client";
import { Params, useParams } from "react-router";
import { Tabs } from "~/routes/$orgnr_.oversikt";
import { Environment } from "~/services/environment";

export function getCurrentTab(request: Request): Tabs {
  return new URL(request.url).searchParams.get("forside-tab") as Tabs;
}

export function requireOrgnr(orgnr?: string): string {
  if (orgnr) {
    return orgnr;
  }
  throw new Error("Mangler orgnr");
}

export function requireGjennomforingId(gjennomforingId?: string): string {
  if (gjennomforingId) {
    return gjennomforingId;
  }
  throw new Error("Mangler gjennomføring id");
}

export function useOrgnrFromUrl(): string {
  const { orgnr } = useParams();
  return requireOrgnr(orgnr);
}

export function useGjennomforingIdFromUrl(): string {
  const { gjennomforingid } = useParams();
  return requireGjennomforingId(gjennomforingid);
}

export function getOrgnrGjennomforingIdFrom(params: Params<string>): {
  orgnr: string;
  gjennomforingId: string;
} {
  const { orgnr, gjennomforingid } = params;
  return { orgnr: requireOrgnr(orgnr), gjennomforingId: requireGjennomforingId(gjennomforingid) };
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
      driftstilskuddv2: {
        innsendingsinformasjon: (gjennomforingId: string) =>
          `/${orgnr}/opprett-krav/${gjennomforingId}/innsendingsinformasjon`,
        deltakere: (gjennomforingId: string) =>
          `/${orgnr}/opprett-krav/${gjennomforingId}/deltakere`,
        utbetaling: (gjennomforingId: string) =>
          `/${orgnr}/opprett-krav/${gjennomforingId}/utbetaling`,
        vedlegg: (gjennomforingId: string) => `/${orgnr}/opprett-krav/${gjennomforingId}/vedlegg`,
        oppsummering: (gjennomforingId: string) =>
          `/${orgnr}/opprett-krav/${gjennomforingId}/oppsummering`,
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

export function pathBySteg(steg: OpprettKravVeiviserSteg, orgnr: string, gjennomforingId: string) {
  switch (steg) {
    case OpprettKravVeiviserSteg.INFORMASJON:
      return pathByOrgnr(orgnr).opprettKrav.driftstilskuddv2.innsendingsinformasjon(
        gjennomforingId,
      );
    case OpprettKravVeiviserSteg.DELTAKERLISTE:
      return pathByOrgnr(orgnr).opprettKrav.driftstilskuddv2.deltakere(gjennomforingId);
    case OpprettKravVeiviserSteg.UTBETALING:
      return pathByOrgnr(orgnr).opprettKrav.driftstilskuddv2.utbetaling(gjennomforingId);
    case OpprettKravVeiviserSteg.VEDLEGG:
      return pathByOrgnr(orgnr).opprettKrav.driftstilskuddv2.vedlegg(gjennomforingId);
    case OpprettKravVeiviserSteg.OPPSUMMERING:
      return pathByOrgnr(orgnr).opprettKrav.driftstilskuddv2.oppsummering(gjennomforingId);
  }
}

export function deltakerOversiktLenke(env: Environment): string {
  if (env === Environment.DevGcp) {
    return "https://amt.intern.dev.nav.no/deltakeroversikt";
  }
  return "https://nav.no/deltakeroversikt";
}
