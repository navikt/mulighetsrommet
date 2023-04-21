import { NavEnhet } from "mulighetsrommet-api-client";

export const hentEnhetsnavn = (
  enheter: NavEnhet[] = [],
  enhetsnummer?: string
): string => {
  if (!enhetsnummer) return "";

  return enheter?.find((e) => e.enhetNr === enhetsnummer)?.navn ?? enhetsnummer;
};

export const hentListeMedEnhetsnavn = (
  enheter: NavEnhet[] = [],
  enhetsnummer?: string[]
): string[] => {
  return (
    enhetsnummer?.map((enhet) => hentEnhetsnavn(enheter, enhet)).sort() ?? []
  );
};
