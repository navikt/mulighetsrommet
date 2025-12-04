import { OpprettKravVeiviserSteg } from "@api-client";
import { Params, useParams } from "react-router";
import { Environment } from "~/services/environment";

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
    utbetalinger: "/",
    opprettKrav: {
      oversikt: `/${orgnr}/opprett-krav/`,
      innsendingsinformasjon: (gjennomforingId: string) =>
        `/${orgnr}/opprett-krav/${gjennomforingId}/innsendingsinformasjon`,
      deltakere: (gjennomforingId: string) => `/${orgnr}/opprett-krav/${gjennomforingId}/deltakere`,
      utbetaling: (gjennomforingId: string) =>
        `/${orgnr}/opprett-krav/${gjennomforingId}/utbetaling`,
      vedlegg: (gjennomforingId: string) => `/${orgnr}/opprett-krav/${gjennomforingId}/vedlegg`,
      oppsummering: (gjennomforingId: string) =>
        `/${orgnr}/opprett-krav/${gjennomforingId}/oppsummering`,
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
      return pathByOrgnr(orgnr).opprettKrav.innsendingsinformasjon(gjennomforingId);
    case OpprettKravVeiviserSteg.DELTAKERLISTE:
      return pathByOrgnr(orgnr).opprettKrav.deltakere(gjennomforingId);
    case OpprettKravVeiviserSteg.UTBETALING:
      return pathByOrgnr(orgnr).opprettKrav.utbetaling(gjennomforingId);
    case OpprettKravVeiviserSteg.VEDLEGG:
      return pathByOrgnr(orgnr).opprettKrav.vedlegg(gjennomforingId);
    case OpprettKravVeiviserSteg.OPPSUMMERING:
      return pathByOrgnr(orgnr).opprettKrav.oppsummering(gjennomforingId);
  }
}

export function deltakerOversiktLenke(env: Environment): string {
  if (env === Environment.DevGcp) {
    return "https://amt.intern.dev.nav.no/deltakeroversikt";
  }
  return "https://nav.no/deltakeroversikt";
}
