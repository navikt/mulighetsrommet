import { Avtale, NavEnhet } from "mulighetsrommet-api-client";

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

export const finnOverordnetEnhetFraAvtale = (
  avtale?: Avtale,
  enheter?: NavEnhet[]
): NavEnhet | undefined => {
  const avtaleEnhet = enheter?.find(
    (e: NavEnhet) => e.enhetNr === avtale?.navEnhet?.enhetsnummer
  );
  if (!avtaleEnhet) {
    return undefined;
  }
  return avtaleEnhet.overordnetEnhet
    ? enheter?.find(
        (e: NavEnhet) => e.overordnetEnhet === avtaleEnhet?.overordnetEnhet
      )
    : enheter?.find(
        (e: NavEnhet) => e.enhetNr === avtale?.navEnhet?.enhetsnummer
      );
};
