import { useParams } from "react-router";
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

export function requireId(id?: string): string {
  if (id) {
    return id;
  }
  throw new Error("Mangler id");
}

export function useOrgnrFromUrl(): string {
  const { orgnr } = useParams();
  return requireOrgnr(orgnr);
}

export function useIdFromUrl(): string {
  const { id } = useParams();
  return requireId(id);
}

export function useGjennomforingIdFromUrl(): string {
  const { gjennomforingid } = useParams();
  return requireGjennomforingId(gjennomforingid);
}

export const pathTo = {
  utbetalinger: "/?forside-tab=aktive",
  tilsagnOversikt: "/?forside-tab=tilsagnsoversikt",
  tiltaksoversikt: "/tiltaksoversikt",
  opprettKrav: (orgnr: string, gjennomforingId: string) =>
    `/${orgnr}/opprett-krav/${gjennomforingId}`,
  innsendingsinformasjon: (orgnr: string, id: string) =>
    `/${orgnr}/utbetaling/${id}/innsendingsinformasjon`,
  beregning: (orgnr: string, id: string) => `/${orgnr}/utbetaling/${id}/beregning`,
  oppsummering: (orgnr: string, id: string) => `/${orgnr}/utbetaling/${id}/oppsummering`,
  kvittering: (orgnr: string, id: string) => `/${orgnr}/utbetaling/${id}/kvittering`,
  detaljer: (orgnr: string, id: string) => `/${orgnr}/utbetaling/${id}/detaljer`,
  tilsagn: (orgnr: string, id: string) => `/${orgnr}/tilsagn/${id}`,
};

export function deltakerOversiktLenke(env: Environment): string {
  if (env === Environment.DevGcp) {
    return "https://amt.intern.dev.nav.no/deltakeroversikt";
  }
  return "https://nav.no/deltakeroversikt";
}
