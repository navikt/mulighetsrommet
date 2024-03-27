import { ArenaNavEnhet, NavEnhet } from "mulighetsrommet-api-client";

export function getDisplayName(enhet: NavEnhet | ArenaNavEnhet) {
  const { enhetsnummer, navn } = enhet;
  return navn ? `${enhetsnummer} ${navn}` : enhetsnummer;
}
