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

export const pathTo = {
  utbetalinger: "/",
  tiltaksOversikt: "/tiltak-oversikt",
  opprettKrav: {
    innsendingsinformasjon: (orgnr: string, gjennomforingId: string) =>
      `/${orgnr}/opprett-krav/${gjennomforingId}/innsendingsinformasjon`,
    deltakere: (orgnr: string, gjennomforingId: string) =>
      `/${orgnr}/opprett-krav/${gjennomforingId}/deltakere`,
    utbetaling: (orgnr: string, gjennomforingId: string) =>
      `/${orgnr}/opprett-krav/${gjennomforingId}/utbetaling`,
    vedlegg: (orgnr: string, gjennomforingId: string) =>
      `/${orgnr}/opprett-krav/${gjennomforingId}/vedlegg`,
    oppsummering: (orgnr: string, gjennomforingId: string) =>
      `/${orgnr}/opprett-krav/${gjennomforingId}/oppsummering`,
  },
  innsendingsinformasjon: (orgnr: string, id: string) =>
    `/${orgnr}/utbetaling/${id}/innsendingsinformasjon`,
  beregning: (orgnr: string, id: string) => `/${orgnr}/utbetaling/${id}/beregning`,
  oppsummering: (orgnr: string, id: string) => `/${orgnr}/utbetaling/${id}/oppsummering`,
  kvittering: (orgnr: string, id: string) => `/${orgnr}/utbetaling/${id}/kvittering`,
  detaljer: (orgnr: string, id: string) => `/${orgnr}/utbetaling/${id}/detaljer`,
  tilsagn: (orgnr: string, id: string) => `/${orgnr}/tilsagn/${id}`,
};

export function pathBySteg(steg: OpprettKravVeiviserSteg, orgnr: string, gjennomforingId: string) {
  switch (steg) {
    case OpprettKravVeiviserSteg.INFORMASJON:
      return pathTo.opprettKrav.innsendingsinformasjon(orgnr, gjennomforingId);
    case OpprettKravVeiviserSteg.DELTAKERLISTE:
      return pathTo.opprettKrav.deltakere(orgnr, gjennomforingId);
    case OpprettKravVeiviserSteg.UTBETALING:
      return pathTo.opprettKrav.utbetaling(orgnr, gjennomforingId);
    case OpprettKravVeiviserSteg.VEDLEGG:
      return pathTo.opprettKrav.vedlegg(orgnr, gjennomforingId);
    case OpprettKravVeiviserSteg.OPPSUMMERING:
      return pathTo.opprettKrav.oppsummering(orgnr, gjennomforingId);
  }
}

export function deltakerOversiktLenke(env: Environment): string {
  if (env === Environment.DevGcp) {
    return "https://amt.intern.dev.nav.no/deltakeroversikt";
  }
  return "https://nav.no/deltakeroversikt";
}
